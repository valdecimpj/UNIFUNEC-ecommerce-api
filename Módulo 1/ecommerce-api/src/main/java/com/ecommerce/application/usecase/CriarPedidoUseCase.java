package com.ecommerce.application.usecase;

import com.ecommerce.application.dto.CriarPedidoDTO;
import com.ecommerce.application.dto.PedidoResponseDTO;
import com.ecommerce.application.event.PedidoCriadoEvent;
import com.ecommerce.application.mapper.PedidoMapper;
import com.ecommerce.domain.exception.ProdutoNaoEncontradoException;
import com.ecommerce.domain.model.ItemPedido;
import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.Produto;
import com.ecommerce.domain.repository.PedidoRepository;
import com.ecommerce.domain.repository.ProdutoRepository;
import com.ecommerce.shared.service.FreteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Caso de uso: Criar Pedido.
 * Orquestra domínio, repositórios e eventos – sem lógica de negócio própria.
 * Responsabilidade única: coordenar o fluxo de criação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CriarPedidoUseCase {

    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final FreteService freteService;
    private final ApplicationEventPublisher eventPublisher;
    private final PedidoMapper pedidoMapper;

    @Transactional
    public PedidoResponseDTO executar(CriarPedidoDTO dto) {
        log.info("Criando pedido para clienteId={}", dto.getClienteId());

        // 1. Monta os itens validando estoque de cada produto no domínio
        List<ItemPedido> itens = dto.getItens().stream()
                .map(itemDTO -> {
                    Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
                            .orElseThrow(() -> new ProdutoNaoEncontradoException(itemDTO.getProdutoId()));

                    // Regra de domínio: valida estoque e captura snapshot do preço
                    produto.validarEstoque(itemDTO.getQuantidade());
                    return ItemPedido.of(produto, itemDTO.getQuantidade());
                })
                .toList();

        // 2. Calcula frete (serviço de domínio separado)
        BigDecimal frete = freteService.calcular(dto.getCep(), itens);

        // 3. Cria o objeto de domínio Pedido
        Pedido pedido = Pedido.builder()
                .clienteId(dto.getClienteId())
                .frete(frete)
                .build();

        itens.forEach(pedido::adicionarItem);

        // 4. Persiste via repositório (abstração do domínio)
        Pedido salvo = pedidoRepository.save(pedido);
        log.info("Pedido criado com id={}", salvo.getId());

        // 5. Publica evento – listeners processam notificação e atualização de estoque
        eventPublisher.publishEvent(new PedidoCriadoEvent(salvo));

        return pedidoMapper.toResponseDTO(salvo);
    }
}
