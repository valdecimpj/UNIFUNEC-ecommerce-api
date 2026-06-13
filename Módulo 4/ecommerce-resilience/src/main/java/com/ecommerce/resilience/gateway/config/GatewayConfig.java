package com.ecommerce.resilience.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Configuração programática do Spring Cloud Gateway.
 *
 * Spring Cloud Gateway é baseado em WebFlux – não-bloqueante por natureza.
 * NUNCA use spring-boot-starter-web junto com o Gateway (conflito de stacks).
 *
 * Componentes:
 *   Route:         define uma rota (predicate + filters + uri)
 *   Predicate:     condição para a rota ser ativada (path, header, method, query...)
 *   GatewayFilter: transforma request/response (add header, rewrite path, retry...)
 *   GlobalFilter:  aplica a TODAS as rotas (auth, logging, tracing)
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            // ── ROTA 1: Order Service ──────────────────────────────────────
            .route("order-service", r -> r
                // Predicate: ativa se o path começar com /api/v1/pedidos
                .path("/api/v1/pedidos/**")
                .filters(f -> f
                    // Rewrite: remove /api/v1 antes de encaminhar
                    .rewritePath("/api/v1/(?<segment>.*)", "/${segment}")
                    // Adiciona header de correlação para rastreamento distribuído
                    .addRequestHeader("X-Gateway-Source", "ecommerce-gateway")
                    // Retry: tenta 3x em caso de 503 (Service Unavailable)
                    .retry(config -> config
                        .setRetries(3)
                        .setStatuses(HttpStatus.SERVICE_UNAVAILABLE)
                        .setMethods(HttpMethod.GET)   // só faz retry em GET (idempotente)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                    // Circuit Breaker no Gateway (usa Resilience4j internamente)
                    .circuitBreaker(config -> config
                        .setName("order-cb")
                        .setFallbackUri("forward:/fallback/pedidos"))
                    // Adiciona tempo de resposta no header de saída
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                )
                // Uri com lb:// = Spring Cloud LoadBalancer resolve o nome no Eureka
                .uri("lb://order-service")
            )

            // ── ROTA 2: Estoque Service ────────────────────────────────────
            // Nota: Rate Limiter via Redis requer spring-boot-starter-data-redis
            // e um servidor Redis rodando. Adicione a dependência e configure
            // spring.data.redis.host/port para habilitar o requestRateLimiter aqui.
            .route("estoque-service", r -> r
                .path("/api/v1/estoque/**")
                .filters(f -> f
                    .rewritePath("/api/v1/(?<segment>.*)", "/${segment}")
                    .retry(config -> config
                        .setRetries(2)
                        .setStatuses(HttpStatus.SERVICE_UNAVAILABLE))
                )
                .uri("lb://estoque-service")
            )

            // ── ROTA 3: Produto Service – público, sem auth ────────────────
            .route("produto-service-publico", r -> r
                .path("/api/v1/produtos/**")
                .and().method(HttpMethod.GET) // apenas GET é público
                .filters(f -> f
                    .addRequestHeader("X-Public-Access", "true")
                    .addResponseHeader("Cache-Control", "public, max-age=300") // 5 min cache
                )
                .uri("lb://produto-service")
            )

            // ── ROTA 4: Rota com condições múltiplas (AND) ────────────────
            .route("admin-route", r -> r
                .path("/api/v1/admin/**")
                .and().header("Authorization") // só se Authorization header presente
                .filters(f -> f
                    .addRequestHeader("X-Admin-Request", "true")
                    .prefixPath("/internal")  // adiciona /internal ao path
                )
                .uri("lb://admin-service")
            )

            // ── ROTA 5: Fallback global ────────────────────────────────────
            .route("fallback-route", r -> r
                .path("/fallback/**")
                .filters(f -> f
                    .setStatus(HttpStatus.SERVICE_UNAVAILABLE)
                )
                .uri("forward:/fallback")
            )

            .build();
    }

}
