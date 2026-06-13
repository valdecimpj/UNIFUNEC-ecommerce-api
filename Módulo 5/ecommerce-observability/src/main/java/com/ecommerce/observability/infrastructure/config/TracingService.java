package com.ecommerce.observability.infrastructure.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Distributed Tracing com Micrometer Tracing.
 *
 * Spring Boot 3 integra Micrometer Tracing nativamente.
 * Substitui o Sleuth (descontinuado no Boot 3).
 *
 * Conceitos:
 *   Trace   → rastreamento completo de uma requisição (ex: toda a cadeia de microsserviços)
 *   Span    → unidade de trabalho dentro de um trace (ex: chamada ao banco, chamada ao serviço)
 *   TraceId → ID único do trace (propagado entre serviços via headers HTTP e Kafka)
 *   SpanId  → ID único do span atual
 *   Baggage → dados que viajam com o trace (ex: userId, tenantId)
 *
 * Propagação automática:
 *   WebClient       → B3 headers (X-B3-TraceId, X-B3-SpanId)
 *   @KafkaListener  → baggage nos headers Kafka
 *   RestTemplate    → TraceContext propagado via interceptor
 *
 * Backend: Zipkin (http://localhost:9411)
 *   Configurado via spring.zipkin.tracing.endpoint no application.yml
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TracingService {

    private final Tracer tracer;
    private final ObservationRegistry observationRegistry;

    // =========================================================================
    // @NewSpan – cria um novo span para o método anotado
    // =========================================================================

    /**
     * @NewSpan: cria um span filho do span atual.
     * O span aparece no Zipkin aninhado sob o trace da requisição HTTP.
     *
     * @SpanTag: adiciona tag ao span (visível no Zipkin como atributo do span).
     * Tags são úteis para filtrar e buscar traces: "produto.id = 42"
     */
    @NewSpan("busca-produto-detalhada")
    public void executarBuscaDetalhada(
            @SpanTag("produto.id") Long produtoId,
            @SpanTag("usuario.id") String userId) {

        log.info("[TRACING] Executando busca detalhada: produto={} user={}",
                produtoId, userId);

        // O span é automaticamente fechado ao sair do método
        // TraceId e SpanId são injetados no MDC → aparecem nos logs
        log.debug("[TRACING] traceId={} spanId={}",
                tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "null",
                tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "null");
    }

    // =========================================================================
    // Span programático – controle total sobre criação e finalização
    // =========================================================================

    /**
     * Cria span manualmente para operações não-Spring (ex: chamadas a libs externas).
     * Útil quando @NewSpan não é suficiente (ex: spans em loops, spans condicionais).
     */
    public void processarCheckoutComSpan(Long pedidoId) {
        // Cria span filho do contexto atual
        var span = tracer.nextSpan()
                .name("processar-checkout")
                .tag("pedido.id", String.valueOf(pedidoId))
                .tag("operacao", "checkout")
                .start();

        try (var ws = tracer.withSpan(span)) { // define como span atual
            log.info("[TRACING] Processando checkout: pedidoId={}", pedidoId);

            // Simula etapas do checkout como eventos no span
            span.event("validacao-iniciada");
            // ... validação ...
            span.event("validacao-concluida");

            span.event("pagamento-iniciado");
            // ... pagamento ...
            span.event("pagamento-processado");

        } catch (Exception ex) {
            span.tag("erro", ex.getMessage());
            span.error(ex);  // marca o span como erro no Zipkin
            throw ex;
        } finally {
            span.end(); // SEMPRE feche o span – evita memory leak
        }
    }

    // =========================================================================
    // Observation API – abstração de alto nível (métricas + traces juntos)
    // =========================================================================

    /**
     * Observation: combina métricas (Timer) e traces (Span) em uma única abstração.
     * É a API recomendada no Spring Boot 3 para instrumentar código de aplicação.
     *
     * Uma Observation gera automaticamente:
     *   - Timer (duração da operação)
     *   - Span no Zipkin (trace distribuído)
     *   - Logs com traceId/spanId no MDC
     */
    public <T> T observar(String nome, String descricao, java.util.function.Supplier<T> operacao) {
        return Observation.createNotStarted(nome, observationRegistry)
                .lowCardinalityKeyValue("servico", "produto")  // tag de baixa cardinalidade
                .highCardinalityKeyValue("operacao", descricao) // tag de alta cardinalidade
                .observe(operacao);
    }
}
