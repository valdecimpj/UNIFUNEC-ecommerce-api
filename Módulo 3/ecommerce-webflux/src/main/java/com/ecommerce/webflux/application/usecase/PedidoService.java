package com.ecommerce.webflux.application.usecase;

import com.ecommerce.webflux.domain.model.ItemPedido;
import com.ecommerce.webflux.domain.model.Pedido;
import com.ecommerce.webflux.domain.model.Produto;
import com.ecommerce.webflux.domain.model.enums.StatusPedido;
import com.ecommerce.webflux.domain.repository.ItemPedidoReactiveRepository;
import com.ecommerce.webflux.domain.repository.PedidoReactiveRepository;
import com.ecommerce.webflux.domain.repository.ProdutoReactiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Serviço reativo de pedidos.
 *
 * DIFERENÇA IMPORTANTE com JPA:
 * R2DBC não tem relacionamentos lazy/eager.
 * Para carregar Pedido com seus Itens, fazemos duas queries
 * e combinamos com zipWith() ou flatMap().
 *
 * Isso é explícito e controlado – sem N+1 silencioso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoReactiveRepository pedidoRepository;
    private final ItemPedidoReactiveRepository itemRepository;
    private final ProdutoReactiveRepository produtoRepository;

    // =========================================================================
    // CRIAR PEDIDO – pipeline reativa completa
    // =========================================================================

    @Transactional
    public Mono<Pedido> criarPedido(Long clienteId, Map<Long, Integer> produtoQuantidade) {
        // 1. Valida e decrementa estoque de cada produto em paralelo
        Flux<ItemPedido> itensFlux = Flux.fromIterable(produtoQuantidade.entrySet())
                .flatMap(entry -> validarECriarItem(entry.getKey(), entry.getValue()));

        // 2. Coleta todos os itens, salva o pedido e depois salva os itens
        return itensFlux
                .collectList()
                .flatMap(itens -> {
                    BigDecimal frete = calcularFrete(itens);

                    Pedido pedido = Pedido.builder()
                            .clienteId(clienteId)
                            .status(StatusPedido.PENDENTE)
                            .frete(frete)
                            .criadoEm(LocalDateTime.now())
                            .build();

                    // Salva o pedido primeiro para obter o ID gerado
                    return pedidoRepository.save(pedido)
                            .flatMap(pedidoSalvo -> {
                                // Associa o pedidoId a cada item e salva todos
                                List<ItemPedido> itensComPedidoId = itens.stream()
                                        .map(item -> {
                                            item.setPedidoId(pedidoSalvo.getId());
                                            return item;
                                        })
                                        .toList();

                                return itemRepository.saveAll(itensComPedidoId)
                                        .collectList()
                                        .map(itensSalvos -> {
                                            pedidoSalvo.setItens(itensSalvos);
                                            return pedidoSalvo;
                                        });
                            });
                })
                .doOnSuccess(p -> log.info("Pedido criado: id={} clienteId={}", p.getId(), clienteId));
    }

    // =========================================================================
    // BUSCAR PEDIDO COM ITENS – join explícito (sem lazy loading)
    // =========================================================================

    /**
     * Carrega pedido + itens em duas queries combinadas com zipWith.
     *
     * zipWith(): espera ambos os publishers completarem e combina os resultados.
     * Ideal quando os dois publishers são independentes (podem rodar em paralelo).
     */
    public Mono<Pedido> buscarPorIdComItens(Long id) {
        Mono<Pedido> pedidoMono = pedidoRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Pedido não encontrado: " + id)));

        Mono<List<ItemPedido>> itensMono = itemRepository.findByPedidoId(id)
                .collectList();

        // Combina pedido + itens quando ambos chegarem
        return pedidoMono.zipWith(itensMono, (pedido, itens) -> {
            pedido.setItens(itens);
            return pedido;
        });
    }

    /**
     * Lista pedidos de um cliente com seus itens (N pedidos + N queries de itens).
     * flatMap() faz a query de itens para cada pedido.
     */
    public Flux<Pedido> listarPorClienteComItens(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId)
                .flatMap(pedido ->
                    itemRepository.findByPedidoId(pedido.getId())
                            .collectList()
                            .map(itens -> {
                                pedido.setItens(itens);
                                return pedido;
                            })
                );
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Mono<ItemPedido> validarECriarItem(Long produtoId, int quantidade) {
        return produtoRepository.findById(produtoId)
                .switchIfEmpty(Mono.error(
                    new RuntimeException("Produto não encontrado: " + produtoId)))
                .flatMap(produto -> {
                    if (produto.getEstoque() < quantidade) {
                        return Mono.error(new RuntimeException(
                            "Estoque insuficiente para: " + produto.getNome()));
                    }
                    return produtoRepository.decrementarEstoque(produtoId, quantidade)
                            .map(ok -> ItemPedido.builder()
                                    .produtoId(produtoId)
                                    .quantidade(quantidade)
                                    .precoUnitario(produto.getPreco())
                                    .build());
                });
    }

    private BigDecimal calcularFrete(List<ItemPedido> itens) {
        BigDecimal subtotal = itens.stream()
                .map(ItemPedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.compareTo(BigDecimal.valueOf(300)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(15.00);
    }
}
