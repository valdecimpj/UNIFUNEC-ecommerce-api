package com.ecommerce.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários dos padrões de resiliência do Resilience4j.
 * Testa o comportamento do Circuit Breaker e Retry diretamente,
 * sem necessidade de subir o contexto Spring.
 */
@DisplayName("Testes de Resiliência – Circuit Breaker e Retry")
class ResiliencePatternTest {

    // =========================================================================
    // CIRCUIT BREAKER
    // =========================================================================

    @Test
    @DisplayName("Circuit Breaker: transita para OPEN após taxa de falha excedida")
    void circuitBreaker_devAbrirAposTaxaDeFalha() {
        // Configura CB com janela de 5 chamadas, abre se ≥60% falhar
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .failureRateThreshold(60.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.of(config)
                .circuitBreaker("test-cb");

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Executa 5 chamadas, 3 falhando (60% de falha)
        for (int i = 0; i < 5; i++) {
            try {
                int finalI = i;
                cb.executeCallable(() -> {
                    if (finalI < 3) throw new RuntimeException("Falha simulada");
                    return "OK";
                });
            } catch (Exception ignored) {}
        }

        // CB deve estar OPEN após 60% de falha
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(cb.getMetrics().getFailureRate()).isGreaterThanOrEqualTo(60.0f);
    }

    @Test
    @DisplayName("Circuit Breaker: rejeita chamadas quando OPEN (falha rápida)")
    void circuitBreaker_deveRejeitarQuandoAberto() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.of(config)
                .circuitBreaker("test-open-cb");

        // Força abertura do circuito
        cb.transitionToOpenState();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Tentativa de chamada com CB OPEN deve lançar CallNotPermittedException imediatamente
        assertThatThrownBy(() ->
            cb.executeRunnable(() -> System.out.println("não deve executar"))
        ).isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
    }

    @Test
    @DisplayName("Circuit Breaker: ignora exceções configuradas (não conta como falha)")
    void circuitBreaker_deveIgnorarExcecoesDeDominio() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .failureRateThreshold(50.0f)
                // EstoqueInsuficienteException não conta como falha técnica
                .ignoreExceptions(IllegalStateException.class)
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.of(config)
                .circuitBreaker("test-ignore-cb");

        // getNumberOfIgnoredErrors() foi removido no Resilience4j 2.x –
        // usa EventPublisher para contar erros ignorados
        AtomicInteger ignoredCount = new AtomicInteger(0);
        cb.getEventPublisher().onIgnoredError(event -> ignoredCount.incrementAndGet());

        // Executa 10 chamadas que lançam IllegalStateException (ignorada)
        for (int i = 0; i < 10; i++) {
            try {
                cb.executeCallable(() -> { throw new IllegalStateException("Negócio"); });
            } catch (Exception ignored) {}
        }

        // CB deve continuar CLOSED pois a exceção é ignorada
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(ignoredCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("Circuit Breaker: HALF_OPEN testa recuperação do serviço")
    void circuitBreaker_halfOpen_deveTestarRecuperacao() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.of(config)
                .circuitBreaker("test-half-open-cb");

        // Força OPEN
        cb.transitionToOpenState();

        // Aguarda transição automática para HALF_OPEN
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // 2 chamadas bem-sucedidas no HALF_OPEN → volta para CLOSED
        cb.executeRunnable(() -> {});
        cb.executeRunnable(() -> {});

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // =========================================================================
    // RETRY
    // =========================================================================

    @Test
    @DisplayName("Retry: executa N vezes antes de propagar a exceção")
    void retry_deveExecutarNVezesAntesDePropagarErro() {
        AtomicInteger tentativas = new AtomicInteger(0);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(RuntimeException.class)
                .build();

        Retry retry = RetryRegistry.of(config).retry("test-retry");

        Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
            tentativas.incrementAndGet();
            throw new RuntimeException("Falha simulada");
        });

        assertThatThrownBy(supplier::get)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Falha simulada");

        // Deve ter tentado exatamente 3 vezes
        assertThat(tentativas.get()).isEqualTo(3);
        assertThat(retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Retry: não faz retry em exceções ignoradas")
    void retry_naoDeveRetriarExcecoesIgnoradas() {
        AtomicInteger tentativas = new AtomicInteger(0);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(RuntimeException.class)
                // IllegalArgumentException NÃO faz retry
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        Retry retry = RetryRegistry.of(config).retry("test-no-retry");

        assertThatThrownBy(() ->
            Retry.decorateRunnable(retry, () -> {
                tentativas.incrementAndGet();
                throw new IllegalArgumentException("Erro de negócio");
            }).run()
        ).isInstanceOf(IllegalArgumentException.class);

        // Deve ter tentado apenas 1 vez (sem retry)
        assertThat(tentativas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Retry: sucesso na segunda tentativa – conta como retried success")
    void retry_sucessoNaSegundaTentativa() {
        AtomicInteger tentativas = new AtomicInteger(0);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .build();

        Retry retry = RetryRegistry.of(config).retry("test-eventual-success");

        String resultado = Retry.decorateSupplier(retry, () -> {
            int t = tentativas.incrementAndGet();
            if (t < 2) throw new RuntimeException("Falha na tentativa " + t);
            return "Sucesso na tentativa " + t;
        }).get();

        assertThat(resultado).isEqualTo("Sucesso na tentativa 2");
        assertThat(tentativas.get()).isEqualTo(2);
        assertThat(retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
    }
}
