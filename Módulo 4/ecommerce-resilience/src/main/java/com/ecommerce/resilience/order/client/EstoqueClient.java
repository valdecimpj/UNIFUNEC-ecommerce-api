package com.ecommerce.resilience.order.client;

import com.ecommerce.resilience.shared.exception.EstoqueInsuficienteException;
import com.ecommerce.resilience.shared.exception.ServicoIndisponivelException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Cliente para o Serviço de Estoque.
 *
 * Aplica todos os padrões de resiliência em camadas (da mais externa para interna):
 *
 *   RateLimiter → CircuitBreaker → TimeLimiter → Retry → Bulkhead
 *
 * A ordem importa – cada anotação envolve a próxima:
 *   1. RateLimiter: rejeita se limite de taxa excedido
 *   2. CircuitBreaker: rejeita se circuito OPEN
 *   3. TimeLimiter: cancela se demorar > timeout
 *   4. Retry: tenta novamente se falhar (com as exceções configuradas)
 *   5. Bulkhead: rejeita se concorrência máxima atingida
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueClient {

    @Qualifier("estoqueWebClient")
    private final WebClient estoqueWebClient;

    // =========================================================================
    // VERIFICAR ESTOQUE – com Circuit Breaker + Retry + Bulkhead
    // =========================================================================

    /**
     * Verifica disponibilidade de estoque para um produto.
     *
     * @CircuitBreaker: monitora falhas. Se ≥50% das 10 últimas calls falharem,
     *   transita para OPEN e chama o fallback imediatamente por 10 segundos.
     *
     * @Retry: antes de propagar a falha, tenta até 3 vezes com backoff exponencial.
     *   500ms → 1s → 2s. Só para ConnectException e TimeoutException.
     *
     * @Bulkhead: limita a 10 chamadas concorrentes. Protege o serviço de estoque
     *   de ser sobrecarregado por picos de tráfego no order-service.
     */
    @CircuitBreaker(name = "estoque-service", fallbackMethod = "verificarEstoqueFallback")
    @Retry(name = "estoque-service")
    @Bulkhead(name = "estoque-service", type = Bulkhead.Type.SEMAPHORE)
    public Mono<Boolean> verificarDisponibilidade(Long produtoId, int quantidade) {
        log.debug("[ESTOQUE] Verificando: produto={} qtd={}", produtoId, quantidade);

        return estoqueWebClient
                .get()
                .uri("/api/v1/estoque/{produtoId}/disponivel?quantidade={qtd}",
                        produtoId, quantidade)
                .retrieve()
                .onStatus(status -> status.value() == 422, resp ->
                    // 422 = Estoque Insuficiente – exceção de negócio (não aciona retry)
                    Mono.error(new EstoqueInsuficienteException(produtoId, 0, quantidade)))
                .onStatus(org.springframework.http.HttpStatusCode::is5xxServerError, resp ->
                    // 5xx = falha técnica – aciona retry e Circuit Breaker
                    Mono.error(new RuntimeException("Estoque-service retornou 5xx")))
                .bodyToMono(Boolean.class)
                .doOnNext(ok -> log.debug("[ESTOQUE] Disponível: {}", ok));
    }

    /**
     * FALLBACK do Circuit Breaker de verificação de estoque.
     *
     * Chamado quando:
     *   - O circuito está OPEN (muitas falhas recentes)
     *   - O Bulkhead está cheio
     *   - Todos os retries falharam
     *
     * Boas práticas de fallback:
     *   ✅ Retorne um valor degradado mas útil (optimistic = assume disponível)
     *   ✅ Logue o motivo do fallback para observabilidade
     *   ❌ Não faça I/O no fallback (pode criar cascata de falhas)
     *   ❌ Não lance exceção no fallback (perde o benefício do padrão)
     */
    public Mono<Boolean> verificarEstoqueFallback(Long produtoId, int quantidade, Throwable ex) {
        log.warn("[CB FALLBACK] verificarDisponibilidade: produto={} | causa: {}",
                produtoId, ex.getMessage());

        // Estratégia: resposta otimista (assume disponível) para não bloquear pedidos
        // Em produção: avalie se é melhor falha aberta ou fechada para o negócio
        return Mono.just(true);
    }

    // =========================================================================
    // DECREMENTAR ESTOQUE – com Circuit Breaker + TimeLimiter
    // =========================================================================

    /**
     * @TimeLimiter: cancela a operação se demorar mais de 3 segundos.
     * Retorna CompletableFuture (exigido pelo TimeLimiter anotado).
     */
    @CircuitBreaker(name = "estoque-service", fallbackMethod = "decrementarFallback")
    @TimeLimiter(name = "estoque-service")
    @Bulkhead(name = "estoque-service", type = Bulkhead.Type.SEMAPHORE)
    public CompletableFuture<Void> decrementarEstoque(Long produtoId, int quantidade) {
        return estoqueWebClient
                .post()
                .uri("/api/v1/estoque/{produtoId}/decrementar?quantidade={qtd}",
                        produtoId, quantidade)
                .retrieve()
                .onStatus(status -> status.value() == 422, resp ->
                    Mono.error(new EstoqueInsuficienteException(produtoId, 0, quantidade)))
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("[ESTOQUE] Decrementado: produto={} qtd={}", produtoId, quantidade))
                .toFuture();
    }

    public CompletableFuture<Void> decrementarFallback(Long produtoId, int quantidade, Throwable ex) {
        log.error("[CB FALLBACK] decrementarEstoque falhou: produto={} | causa: {}",
                produtoId, ex.getMessage());
        // Propaga o erro – decremento não pode ser silenciado
        return CompletableFuture.failedFuture(
                new ServicoIndisponivelException("estoque-service"));
    }

    // =========================================================================
    // RATE LIMITER – endpoint público com limite de taxa
    // =========================================================================

    /**
     * @RateLimiter: limita a 100 chamadas/segundo para a API pública.
     * Se excedido, chama o rateLimiterFallback imediatamente (sem esperar).
     */
    @RateLimiter(name = "api-publica", fallbackMethod = "consultarPrecoFallback")
    @CircuitBreaker(name = "estoque-service", fallbackMethod = "consultarPrecoCircuitFallback")
    public Mono<Double> consultarPreco(Long produtoId) {
        return estoqueWebClient
                .get()
                .uri("/api/v1/produtos/{id}/preco", produtoId)
                .retrieve()
                .bodyToMono(Double.class);
    }

    public Mono<Double> consultarPrecoFallback(Long produtoId, Throwable ex) {
        log.warn("[RATE LIMIT] Limite excedido para consultarPreco: produto={}", produtoId);
        return Mono.error(new RuntimeException("Limite de requisições excedido. Tente em 1 segundo."));
    }

    public Mono<Double> consultarPrecoCircuitFallback(Long produtoId, Throwable ex) {
        log.warn("[CB FALLBACK] consultarPreco: produto={}", produtoId);
        return Mono.just(-1.0); // preço inválido como sinal de indisponibilidade
    }
}
