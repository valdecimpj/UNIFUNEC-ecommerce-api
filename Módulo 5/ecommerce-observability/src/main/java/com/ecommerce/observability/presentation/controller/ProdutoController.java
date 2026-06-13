package com.ecommerce.observability.presentation.controller;

import com.ecommerce.observability.application.service.ProdutoService;
import com.ecommerce.observability.domain.model.Produto;
import com.ecommerce.observability.infrastructure.config.TracingService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller com instrumentação completa de observabilidade.
 *
 * @Timed: instrumenta o método com um Timer Micrometer.
 * Gera métricas de:
 *   - Contagem de chamadas
 *   - Duração (p50, p95, p99)
 *   - Erros
 *
 * As métricas ficam disponíveis em /actuator/prometheus no formato:
 *   produto_controller_buscar_seconds_count
 *   produto_controller_buscar_seconds_sum
 *   produto_controller_buscar_seconds_bucket
 *
 * No Grafana:
 *   rate(produto_controller_buscar_seconds_count[5m]) → req/s
 *   histogram_quantile(0.95, ..._bucket[5m])          → p95 latência
 */
@RestController
@RequestMapping("/api/v1/produtos")
@Slf4j
@Tag(name = "Produtos (Observabilidade)", description = "CRUD com métricas, tracing e cache")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final TracingService tracingService;
    private final Counter requisicoesPorStatus;
    private final Counter errosCounter;

    public ProdutoController(ProdutoService produtoService,
                              TracingService tracingService,
                              MeterRegistry registry) {
        this.produtoService  = produtoService;
        this.tracingService  = tracingService;

        this.requisicoesPorStatus = Counter.builder("produto.requisicoes")
                .description("Requisições ao endpoint de produtos")
                .tag("endpoint", "produtos")
                .register(registry);

        this.errosCounter = Counter.builder("produto.erros")
                .description("Erros nas requisições de produto")
                .tag("endpoint", "produtos")
                .register(registry);
    }

    @GetMapping("/{id}")
    @Timed(
        value       = "produto.controller.buscar",
        description = "Tempo de busca de produto por ID",
        percentiles = {0.5, 0.95, 0.99},
        histogram   = true   // publica histograma para Prometheus
    )
    @Operation(summary = "Busca produto por ID – com @Cacheable e @Timed")
    public Produto buscarPorId(@PathVariable Long id) {
        requisicoesPorStatus.increment();
        try {
            return tracingService.observar(
                "produto.buscar.completo",
                "Busca produto ID=" + id,
                () -> produtoService.buscarPorId(id)
            );
        } catch (Exception ex) {
            errosCounter.increment();
            throw ex;
        }
    }

    @GetMapping
    @Timed(value = "produto.controller.listar", description = "Listagem com filtros")
    @Operation(summary = "Listagem paginada com filtros dinâmicos (Specification)")
    public Page<Produto> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20, sort = "nome",
                             direction = Sort.Direction.ASC) Pageable pageable) {

        return produtoService.buscarComFiltros(nome, precoMin, precoMax, ativo, categoriaId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Timed(value = "produto.controller.criar", description = "Criação de produto")
    @Operation(summary = "Cria produto – com @Caching (CachePut + CacheEvict)")
    public Produto criar(@RequestBody Produto produto) {
        return produtoService.criar(produto);
    }

    @PutMapping("/{id}")
    @Timed(value = "produto.controller.atualizar", description = "Atualização de produto")
    @Operation(summary = "Atualiza produto – com @CachePut")
    public Produto atualizar(@PathVariable Long id, @RequestBody Produto produto) {
        produto.setId(id);
        return produtoService.atualizar(produto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Timed(value = "produto.controller.deletar", description = "Deleção de produto")
    @Operation(summary = "Deleta produto – com @CacheEvict")
    public void deletar(@PathVariable Long id) {
        produtoService.deletar(id);
    }

    @DeleteMapping("/cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Invalida todo o cache de produtos manualmente")
    public void invalidarCache() {
        produtoService.invalidarTodoCache();
        log.info("[CACHE] Cache de produtos invalidado manualmente via API");
    }

    @GetMapping("/resumo")
    @Operation(summary = "Lista produtos como projection (id, nome, preco apenas)")
    public java.util.List<?> listarResumo() {
        return produtoService.listarResumo();
    }

    @GetMapping("/metricas")
    @Operation(summary = "Endpoint de demonstração de métricas customizadas")
    public Map<String, Object> metricas() {
        return Map.of(
            "dica", "Acesse /actuator/prometheus para ver todas as métricas",
            "endpoints_observabilidade", Map.of(
                "health",     "/actuator/health",
                "metrics",    "/actuator/metrics",
                "prometheus", "/actuator/prometheus",
                "info",       "/actuator/info",
                "caches",     "/actuator/caches",
                "loggers",    "/actuator/loggers",
                "threaddump", "/actuator/threaddump"
            ),
            "metricas_customizadas", Map.of(
                "produtos_ativos",      "ecommerce.produtos.ativos.total",
                "pedidos_fila",         "ecommerce.pedidos.fila.tamanho",
                "checkout_duracao",     "ecommerce.checkout.duracao",
                "produto_criado_total", "produto.criado.total"
            )
        );
    }
}
