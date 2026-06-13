package com.ecommerce.webflux.presentation.controller;

import com.ecommerce.webflux.application.usecase.ProdutoService;
import com.ecommerce.webflux.domain.model.Produto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Controller WebFlux – estilo anotado (@RestController).
 *
 * DIFERENÇAS para Spring MVC:
 *   - Retorno: Mono<T> / Flux<T> em vez de T / List<T>
 *   - MediaType TEXT_EVENT_STREAM_VALUE: habilita SSE (Server-Sent Events)
 *   - @RestController funciona normalmente – WebFlux reconhece os tipos reativos
 *   - Não use void ou tipos síncronos – quebra o pipeline reativo
 *
 * Quando usar Mono vs Flux:
 *   Mono<T>    → GET por ID, POST, PUT, DELETE (0 ou 1 resultado)
 *   Flux<T>    → GET listagem, streaming contínuo (0..N resultados)
 */
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos (WebFlux)", description = "Controller reativo com Mono e Flux")
public class ProdutoController {

    private final ProdutoService produtoService;

    // ─────────────────────────────────────────────────────────────
    // Mono<T> – endpoints que retornam um único recurso
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /produtos/{id} → Mono<Produto>
     * WebFlux subscreve o Mono e serializa para JSON quando completo.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Busca produto por ID – retorna Mono<Produto>")
    public Mono<Produto> buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id);
        // Se o Mono for vazio, Spring retorna 404 automaticamente
    }

    /**
     * POST /produtos → Mono<Produto> com status 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria produto – retorna Mono<Produto>")
    public Mono<Produto> criar(@RequestBody Produto dto) {
        return produtoService.criar(dto.getNome(), dto.getDescricao(),
                dto.getPreco(), dto.getEstoque());
    }

    /**
     * PATCH /produtos/{id}/preco → Mono<Produto>
     * flatMap no service garante que a persistência é assíncrona.
     */
    @PatchMapping("/{id}/preco")
    @Operation(summary = "Atualiza preço – retorna Mono<Produto>")
    public Mono<Produto> atualizarPreco(@PathVariable Long id,
                                         @RequestParam BigDecimal preco) {
        return produtoService.atualizarPreco(id, preco);
    }

    /**
     * Resumo do estoque combinando dois Monos com zip().
     */
    @GetMapping("/resumo-estoque")
    @Operation(summary = "Resumo de estoque – combina dois Monos com zip()")
    public Mono<String> resumoEstoque() {
        return produtoService.resumoEstoque();
    }

    // ─────────────────────────────────────────────────────────────
    // Flux<T> – endpoints que retornam múltiplos recursos
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /produtos → Flux<Produto>
     * WebFlux serializa como array JSON [{ }, { }, ...].
     */
    @GetMapping
    @Operation(summary = "Lista produtos ativos – retorna Flux<Produto>")
    public Flux<Produto> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "50") int limite) {
        return produtoService.buscarDisponiveis(nome, limite);
    }

    // ─────────────────────────────────────────────────────────────
    // SERVER-SENT EVENTS (SSE) – streaming em tempo real
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /produtos/stream → Flux<Produto> como SSE
     *
     * MediaType TEXT_EVENT_STREAM_VALUE:
     *   - Mantém conexão HTTP aberta
     *   - Cada elemento do Flux é enviado como "data: {...}\n\n"
     *   - Cliente recebe em tempo real sem polling
     *   - Ideal para dashboards ao vivo, notificações, feeds
     *
     * Diferente de array JSON: não precisa esperar todos os elementos.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming SSE – cada produto enviado em tempo real")
    public Flux<Produto> streamProdutos() {
        return produtoService.listarAtivos()
                // Simula delay entre eventos (como se viessem de uma fonte contínua)
                .delayElements(Duration.ofMillis(200));
    }

    /**
     * Stream de preços com atualização a cada segundo.
     * Simula feed de preços em tempo real (ex: dashboard de estoque).
     */
    @GetMapping(value = "/{id}/preco-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE – streaming do preço de um produto")
    public Flux<BigDecimal> streamPreco(@PathVariable Long id) {
        return Flux.interval(Duration.ofSeconds(1)) // emite 0, 1, 2, 3... a cada segundo
                .flatMap(tick -> produtoService.buscarPorId(id))
                .map(Produto::getPreco)
                .distinctUntilChanged() // só emite quando o preço muda
                .take(60); // máximo 60 eventos (1 minuto)
    }
}
