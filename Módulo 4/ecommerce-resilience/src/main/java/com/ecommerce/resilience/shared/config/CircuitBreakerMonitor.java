package com.ecommerce.resilience.shared.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Monitor de eventos do Circuit Breaker.
 *
 * Estados do Circuit Breaker:
 *
 *   CLOSED     → estado normal, chamadas passam livremente
 *               Monitora taxa de falha e chamadas lentas.
 *
 *   OPEN       → circuito "queimado", todas as chamadas são rejeitadas
 *               imediatamente (fallback é invocado).
 *               Aguarda waitDurationInOpenState antes de tentar HALF_OPEN.
 *
 *   HALF_OPEN  → estado de sondagem, permite N chamadas de teste
 *               (permittedNumberOfCallsInHalfOpenState).
 *               Se passar: volta para CLOSED.
 *               Se falhar: volta para OPEN.
 *
 *   DISABLED   → CB desativado (sem proteção) – use apenas em debug
 *   FORCED_OPEN → sempre OPEN (útil para manutenção)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerMonitor {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registrarListeners() {
        // Registra listener para TODOS os Circuit Breakers configurados
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(this::registrarEventos);

        // Registra também para CBs criados dinamicamente no futuro
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> registrarEventos(event.getAddedEntry()));
    }

    private void registrarEventos(CircuitBreaker cb) {
        String nome = cb.getName();

        // Transição de estado – mais importante para alertas e observabilidade
        cb.getEventPublisher()
                .onStateTransition(event -> {
                    CircuitBreaker.State de   = event.getStateTransition().getFromState();
                    CircuitBreaker.State para = event.getStateTransition().getToState();

                    if (para == CircuitBreaker.State.OPEN) {
                        // ALERTA: circuit breaker abriu – serviço provavelmente com problemas
                        log.error("[CB] {} ABRIU! {} → {} | Hora: {}",
                                nome, de, para, event.getCreationTime());
                        // Em produção: envie alerta para Slack/PagerDuty aqui
                    } else if (para == CircuitBreaker.State.HALF_OPEN) {
                        log.warn("[CB] {} em sondagem HALF_OPEN | {} → {}",
                                nome, de, para);
                    } else if (para == CircuitBreaker.State.CLOSED) {
                        log.info("[CB] {} FECHOU (recuperado)! {} → {}",
                                nome, de, para);
                    }
                })

                // Chamada com sucesso
                .onSuccess(event ->
                    log.debug("[CB] {} sucesso em {}ms", nome,
                            event.getElapsedDuration().toMillis()))

                // Chamada com erro (conta para taxa de falha)
                .onError(event ->
                    log.warn("[CB] {} ERRO: {} | {}ms",
                            nome, event.getThrowable().getMessage(),
                            event.getElapsedDuration().toMillis()))

                // Chamada ignorada (não conta para taxa de falha – ex: EstoqueInsuficienteException)
                .onIgnoredError(event ->
                    log.debug("[CB] {} erro IGNORADO: {}",
                            nome, event.getThrowable().getClass().getSimpleName()))

                // Chamada rejeitada (circuito OPEN ou Bulkhead cheio)
                .onCallNotPermitted(event ->
                    log.warn("[CB] {} chamada REJEITADA (circuito {})",
                            nome, cb.getState()))

                // Chamada lenta (acima de slowCallDurationThreshold)
                .onSlowCallRateExceeded(event ->
                    log.warn("[CB] {} taxa de chamadas lentas: {}%",
                            nome, event.getSlowCallRate()));
    }

    /**
     * Consulta o estado atual de um Circuit Breaker pelo nome.
     * Útil para endpoints de health check customizados.
     */
    public CircuitBreaker.State consultarEstado(String nome) {
        return circuitBreakerRegistry.find(nome)
                .map(CircuitBreaker::getState)
                .orElse(null);
    }

    /**
     * Métricas detalhadas de um Circuit Breaker.
     */
    public String resumoMetricas(String nome) {
        return circuitBreakerRegistry.find(nome)
                .map(cb -> {
                    CircuitBreaker.Metrics m = cb.getMetrics();
                    return String.format(
                        "CB[%s] estado=%s | falhas=%.1f%% | lentas=%.1f%% | bufferSize=%d",
                        nome,
                        cb.getState(),
                        m.getFailureRate(),
                        m.getSlowCallRate(),
                        m.getNumberOfBufferedCalls()
                    );
                })
                .orElse("Circuit Breaker não encontrado: " + nome);
    }
}
