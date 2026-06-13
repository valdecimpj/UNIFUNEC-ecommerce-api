package com.ecommerce.observability.infrastructure.config;

import com.ecommerce.observability.domain.repository.ProdutoRepository;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuração de métricas customizadas com Micrometer.
 *
 * Tipos de métricas disponíveis:
 *
 *   Counter           → valor que só cresce (ex: total de pedidos criados)
 *   Gauge             → valor que sobe e desce (ex: usuários online, tamanho de fila)
 *   Timer             → mede duração e conta chamadas (ex: latência de endpoint)
 *   DistributionSummary → mede tamanho/distribuição (ex: tamanho de payload em bytes)
 *   LongTaskTimer     → mede tarefas longas em andamento
 *
 * Todos são exportados automaticamente para Prometheus via /actuator/prometheus.
 * Tags dimensionais permitem filtrar e agrupar no Grafana:
 *   rate(produto_criado_total[5m]) by (origem)
 */
@Configuration
@Slf4j
public class MetricsConfig {

    // =========================================================================
    // GAUGE – valor que sobe e desce
    // =========================================================================

    /**
     * MeterBinder é a forma recomendada de registrar métricas que precisam
     * de acesso a beans Spring (evita problemas de ordem de inicialização).
     *
     * Este Gauge monitora o total de produtos ativos no banco em tempo real.
     * Prometheus consulta o valor a cada scrape (ex: a cada 60 segundos).
     */
    @Bean
    public MeterBinder produtosAtivosBinder(ProdutoRepository produtoRepository) {
        return registry -> {
            // Gauge: valor consultado a cada scrape do Prometheus
            Gauge.builder("ecommerce.produtos.ativos.total",
                          produtoRepository,
                          repo -> repo.countByAtivoTrue())
                    .description("Total de produtos ativos no catálogo")
                    .tag("tipo", "catalogo")
                    .register(registry);

            log.info("[METRICS] Gauge ecommerce.produtos.ativos.total registrado");
        };
    }

    /**
     * Gauge para monitorar o tamanho de uma fila em memória.
     * Útil para detectar backpressure ou processamento lento.
     */
    @Bean
    public AtomicInteger filaPedidosPendentes(MeterRegistry registry) {
        AtomicInteger fila = new AtomicInteger(0);

        Gauge.builder("ecommerce.pedidos.fila.tamanho", fila, AtomicInteger::get)
                .description("Pedidos aguardando processamento na fila em memória")
                .tag("fila", "processamento")
                .register(registry);

        return fila;
    }

    // =========================================================================
    // TIMER – mede duração + distribuição (p50, p95, p99)
    // =========================================================================

    /**
     * Timer com percentis configurados.
     * publishPercentiles: calcula no cliente (menos preciso, sem overhead de servidor)
     * publishPercentileHistogram: envia histograma para Prometheus (mais preciso)
     *
     * No Grafana, use:
     *   histogram_quantile(0.95, rate(checkout_duracao_seconds_bucket[5m]))
     */
    @Bean
    public Timer checkoutTimer(MeterRegistry registry) {
        return Timer.builder("ecommerce.checkout.duracao")
                .description("Duração do processo de checkout completo")
                .tag("fluxo", "checkout")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99) // p50, p75, p95, p99
                .publishPercentileHistogram(true)
                .minimumExpectedValue(java.time.Duration.ofMillis(10))
                .maximumExpectedValue(java.time.Duration.ofSeconds(30))
                .register(registry);
    }

    // =========================================================================
    // DISTRIBUTION SUMMARY – distribuição de valores (ex: tamanho de payload)
    // =========================================================================

    /**
     * DistributionSummary: como um Timer mas para valores sem unidade de tempo.
     * Ex: tamanho de pedido (número de itens), valor de compra (R$).
     */
    @Bean
    public DistributionSummary pedidoValorSummary(MeterRegistry registry) {
        return DistributionSummary.builder("ecommerce.pedido.valor")
                .description("Distribuição do valor dos pedidos em reais")
                .baseUnit("reais")
                .tag("moeda", "BRL")
                .publishPercentiles(0.5, 0.9, 0.99)
                .scale(1.0)
                .register(registry);
    }

    // =========================================================================
    // CONFIGURAÇÃO GLOBAL DE TAGS (aplica a TODAS as métricas)
    // =========================================================================

    /**
     * MeterFilter: modifica métricas globalmente.
     * Aqui: adiciona tags comuns a todas as métricas do domínio ecommerce.
     *
     * No application.yml já configuramos tags globais via management.metrics.tags.
     * MeterFilter permite lógica mais complexa (ex: filtrar métricas, renomear).
     */
    @Bean
    public MeterFilter dominioTagFilter() {
        return MeterFilter.commonTags(
                Tags.of("dominio", "ecommerce", "versao", "v1")
        );
    }

    /**
     * MeterFilter: desativa métricas muito verbosas que não agregam valor.
     * Reduz overhead de coleta e tamanho do scrape do Prometheus.
     */
    @Bean
    public MeterFilter desativarMetricasVerbosas() {
        return MeterFilter.deny(id ->
            id.getName().startsWith("jvm.gc.pause")  // GC pause é coberto por outros
        );
    }
}
