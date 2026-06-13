package com.ecommerce.webflux.presentation.controller;

import com.ecommerce.webflux.application.usecase.AvaliacaoService;
import com.ecommerce.webflux.domain.model.Avaliacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/avaliacoes")
@RequiredArgsConstructor
@Tag(name = "Avaliações (MongoDB Reativo)", description = "CRUD reativo com MongoDB e Change Streams")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria avaliação no MongoDB")
    public Mono<Avaliacao> criar(@RequestBody Avaliacao avaliacao) {
        return avaliacaoService.criar(avaliacao);
    }

    @GetMapping("/produto/{produtoId}")
    @Operation(summary = "Lista avaliações aprovadas de um produto")
    public Flux<Avaliacao> listarAprovadas(@PathVariable Long produtoId) {
        return avaliacaoService.listarAprovadas(produtoId);
    }

    @GetMapping("/produto/{produtoId}/media")
    @Operation(summary = "Média das notas de um produto")
    public Mono<Double> mediaNotas(@PathVariable Long produtoId) {
        return avaliacaoService.calcularMediaNotas(produtoId);
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(summary = "Aprova avaliação para exibição")
    public Mono<Avaliacao> aprovar(@PathVariable String id) {
        return avaliacaoService.aprovar(id);
    }

    /**
     * SSE com Change Stream – feed ao vivo de novas avaliações.
     * Cada vez que alguém postar uma avaliação, todos os clientes
     * conectados a este endpoint recebem o evento em tempo real.
     *
     * Use no frontend com EventSource:
     *   const source = new EventSource('/api/v1/avaliacoes/stream');
     *   source.onmessage = (e) => console.log(JSON.parse(e.data));
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE – stream de novas avaliações via Change Stream")
    public Flux<Avaliacao> streamNovas() {
        return avaliacaoService.streamNovasAvaliacoes();
    }

    @GetMapping(value = "/produto/{produtoId}/stream-aprovadas",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE – stream de avaliações aprovadas (Tailable Cursor)")
    public Flux<Avaliacao> streamAprovadas(@PathVariable Long produtoId) {
        return avaliacaoService.streamAvaliacoesAprovadas()
                .filter(a -> a.getProdutoId().equals(produtoId));
    }
}
