package com.ecommerce.resilience.shared.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Configuração do WebClient para comunicação entre microsserviços.
 *
 * @LoadBalanced: resolve nomes lógicos de serviço (ex: "estoque-service")
 * para instâncias reais via Spring Cloud LoadBalancer + Eureka.
 *
 * Sem @LoadBalanced, precisaria usar URLs fixas (http://localhost:8082).
 * Com @LoadBalanced: WebClient resolve "http://estoque-service/api/..."
 * para a instância real registrada no Eureka.
 */
@Configuration
@Slf4j
public class WebClientConfig {

    /**
     * WebClient com Load Balancing automático.
     * Algoritmo padrão: Round Robin.
     * Alternativas: Random, configurável via ServiceInstanceListSupplier.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Timeout de conexão e leitura
                .clientConnector(new org.springframework.http.client.reactive
                        .ReactorClientHttpConnector(
                            reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(5))
                        ))
                // Filtro de logging de request/response
                .filter(logRequest())
                .filter(logResponse());
    }

    /**
     * WebClient para serviço de estoque (URL base pré-configurada).
     * Usa o nome lógico do Eureka – LoadBalancer resolve para instância real.
     */
    @Bean("estoqueWebClient")
    public WebClient estoqueWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://estoque-service")  // nome lógico no Eureka
                .build();
    }

    /**
     * WebClient para serviço de pagamento.
     */
    @Bean("pagamentoWebClient")
    public WebClient pagamentoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://pagamento-service")
                .build();
    }

    // ── Filtros de logging ────────────────────────────────────

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("[WebClient] → {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.debug("[WebClient] ← Status: {}", resp.statusCode());
            return Mono.just(resp);
        });
    }
}
