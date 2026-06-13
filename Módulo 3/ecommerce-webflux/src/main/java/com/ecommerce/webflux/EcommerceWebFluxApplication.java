package com.ecommerce.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Aplicação Spring WebFlux.
 *
 * Diferenças principais em relação ao Spring MVC:
 *   - Servidor: Netty (event-loop) em vez de Tomcat (thread-per-request)
 *   - Stack: WebFlux + R2DBC + MongoDB Reativo (tudo não-bloqueante)
 *   - Tipos de retorno: Mono<T> e Flux<T> em vez de T e List<T>
 *   - NUNCA use .block() em produção – quebra o modelo reativo
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EcommerceWebFluxApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceWebFluxApplication.class, args);
    }
}
