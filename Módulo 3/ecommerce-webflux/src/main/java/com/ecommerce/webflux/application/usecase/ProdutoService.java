package com.ecommerce.webflux.application.usecase;

import com.ecommerce.webflux.domain.model.Produto;
import com.ecommerce.webflux.domain.repository.ProdutoReactiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;

/**
 * Serviço reativo de produtos.
 *
 * REGRA DE OURO DO WEBFLUX:
 *   NUNCA use .block() em código de produção!
 *   .block() transforma código reativo em bloqueante, desperdiçando
 *   o benefício do event-loop e podendo causar deadlock.
 *
 * Guia de operadores:
 *   map()        → transforma 1 elemento em 1 elemento (síncrono)
 *   flatMap()    → transforma 1 elemento em 0..N (assíncrono, permite inner Mono/Flux)
 *   filter()     → filtra elementos que satisfazem o predicado
 *   switchIfEmpty() → alternativa se o publisher estiver vazio
 *   onErrorReturn() → valor padrão em caso de erro
 *   onErrorResume() → publisher alternativo em caso de erro
 *   doOnNext()   → side-effect sem transformar (ex: logging)
 *   zipWith()    → combina dois publishers elemento a elemento
 *   zip()        → combina múltiplos publishers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    private final ProdutoReactiveRepository produtoRepository;

    // =========================================================================
    // Mono<T> – retorna 0 ou 1 elemento
    // =========================================================================

    /**
     * Busca produto por ID.
     * Mono<Produto>: emite o produto encontrado ou completa vazio.
     * switchIfEmpty transforma "vazio" em erro de domínio.
     */
    public Mono<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .doOnNext(p -> log.debug("Produto encontrado: id={}", p.getId()))
                .switchIfEmpty(Mono.error(
                    new RuntimeException("Produto não encontrado: " + id)));
    }

    /**
     * Cria produto.
     * map() transforma o DTO em entidade (síncrono – não envolve I/O).
     * flatMap() persiste e retorna Mono<Produto> (assíncrono – envolve I/O).
     */
    @Transactional
    public Mono<Produto> criar(String nome, String descricao,
                                BigDecimal preco, Integer estoque) {
        Produto produto = Produto.builder()
                .nome(nome)
                .descricao(descricao)
                .preco(preco)
                .estoque(estoque)
                .ativo(true)
                .build();

        return produtoRepository.save(produto)
                .doOnSuccess(p -> log.info("Produto criado: id={} nome={}", p.getId(), p.getNome()));
    }

    /**
     * Atualização parcial reativa.
     * flatMap() é necessário pois save() retorna Mono<Produto> (assíncrono).
     * Se usássemos map(), teríamos Mono<Mono<Produto>> – incorreto.
     */
    @Transactional
    public Mono<Produto> atualizarPreco(Long id, BigDecimal novoPreco) {
        return produtoRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Produto não encontrado: " + id)))
                .flatMap(produto -> {
                    produto.setPreco(novoPreco);
                    return produtoRepository.save(produto); // assíncrono → flatMap
                })
                .doOnSuccess(p -> log.info("Preço atualizado: id={} novoPreco={}", p.getId(), novoPreco));
    }

    /**
     * Decrementa estoque via query SQL otimizada.
     * Retorna Mono<Boolean> – true se atualizado, false se estoque insuficiente.
     */
    @Transactional
    public Mono<Boolean> decrementarEstoque(Long id, int quantidade) {
        return produtoRepository.decrementarEstoque(id, quantidade)
                .map(linhasAfetadas -> linhasAfetadas > 0)
                .doOnNext(sucesso -> log.info("Estoque decrementado: id={} qtd={} sucesso={}",
                        id, quantidade, sucesso));
    }

    // =========================================================================
    // Flux<T> – retorna 0..N elementos
    // =========================================================================

    /**
     * Lista todos os produtos ativos.
     * Flux<Produto>: emite cada produto individualmente (streaming).
     * filter() é executado para cada elemento emitido.
     */
    public Flux<Produto> listarAtivos() {
        return produtoRepository.findByAtivoTrue()
                .doOnNext(p -> log.debug("Emitindo produto: {}", p.getNome()));
    }

    /**
     * Busca com múltiplos critérios usando operadores de composição.
     * Demonstra: filter, sort, take (limitação de resultados).
     */
    public Flux<Produto> buscarDisponiveis(String nome, int limite) {
        Flux<Produto> fluxBase = (nome != null && !nome.isBlank())
                ? produtoRepository.findByNomeContainingIgnoreCase(nome)
                : produtoRepository.findByAtivoTrue();

        return fluxBase
                .filter(Produto::estaDisponivel)          // só com estoque > 0
                .sort(Comparator.comparing(Produto::getPreco)) // ordena por preço
                .take(limite);                              // limita resultados
    }

    /**
     * Combina busca por faixa de preço com enriquecimento de dados.
     * zipWith() combina dois Monos em um único resultado.
     */
    public Mono<String> resumoEstoque() {
        Mono<Long> totalAtivos  = produtoRepository.countByAtivoTrue();
        Mono<Long> comEstoque   = produtoRepository.findByEstoqueGreaterThan(0).count();

        // zip() espera os dois Monos completarem e combina os resultados
        return Mono.zip(totalAtivos, comEstoque)
                .map(tuple -> String.format(
                    "Produtos ativos: %d | Com estoque: %d",
                    tuple.getT1(), tuple.getT2()));
    }

    /**
     * Operações em paralelo com flatMap e Schedulers.
     * Busca detalhes de múltiplos produtos simultaneamente.
     */
    public Flux<Produto> buscarMultiplos(java.util.List<Long> ids) {
        return Flux.fromIterable(ids)
                // flatMap com concurrency=4: até 4 buscas simultâneas
                .flatMap(id -> produtoRepository.findById(id)
                        .subscribeOn(Schedulers.boundedElastic()), // pool para I/O
                        4) // concurrencyHint
                .filter(Produto::estaDisponivel);
    }

    /**
     * Tratamento de erros reativos.
     * onErrorResume: substitui o erro por um publisher alternativo.
     * onErrorReturn: substitui o erro por um valor padrão.
     * retry: tenta novamente N vezes antes de propagar o erro.
     */
    public Mono<Produto> buscarComFallback(Long id) {
        return produtoRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Não encontrado")))
                // Tenta 2 vezes com delay de 100ms entre tentativas
                .retryWhen(reactor.util.retry.Retry
                        .fixedDelay(2, Duration.ofMillis(100)))
                // Se ainda assim falhar, retorna produto padrão
                .onErrorResume(ex -> {
                    log.warn("Produto {} não encontrado após retries. Usando fallback.", id);
                    return Mono.just(Produto.builder()
                            .id(id)
                            .nome("Produto Indisponível")
                            .preco(BigDecimal.ZERO)
                            .estoque(0)
                            .ativo(false)
                            .build());
                });
    }

    /**
     * Demonstra Schedulers para tarefas bloqueantes legadas.
     * Se precisar chamar código bloqueante (ex: API antiga sem suporte reativo),
     * use Schedulers.boundedElastic() para não bloquear o event-loop.
     */
    public Mono<String> chamarApiLegadoBloqueante(Long id) {
        return Mono.fromCallable(() -> {
                    // ⚠️ Código bloqueante (ex: RestTemplate, JDBC legado)
                    // Executado em thread do boundedElastic, não no event-loop
                    Thread.sleep(100); // simula chamada bloqueante
                    return "Dados da API legada para produto " + id;
                })
                .subscribeOn(Schedulers.boundedElastic()); // move para pool dedicado
    }
}
