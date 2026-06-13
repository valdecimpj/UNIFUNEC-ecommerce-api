package com.ecommerce.resilience.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * GlobalFilter – aplica a TODAS as rotas do Gateway.
 *
 * Responsabilidades:
 *   1. Injeta X-Correlation-ID em toda requisição (rastreamento distribuído)
 *   2. Loga entrada e saída de cada request
 *   3. Mede tempo de resposta
 *
 * Ordem de execução: Ordered.HIGHEST_PRECEDENCE = executa primeiro
 */
@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();

        // Gera ou propaga X-Correlation-ID para rastreamento end-to-end
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst("X-Correlation-ID");

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Injeta o correlation ID no request que será enviado ao microsserviço
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Correlation-ID", finalCorrelationId)
                .build();

        log.info("[GATEWAY] → {} {} | correlationId={}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    long duracao = System.currentTimeMillis() - inicio;
                    int status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("[GATEWAY] ← {} {} | status={} | {}ms | correlationId={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath(),
                            status,
                            duracao,
                            finalCorrelationId);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // executa antes de todos os outros filtros
    }
}
