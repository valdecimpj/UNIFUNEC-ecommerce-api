package com.ecommerce.resilience;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Módulo 4 – Resiliência e Microsserviços
 *
 * Este projeto demonstra em um único módulo:
 *   - Circuit Breaker com Resilience4j (CLOSED → OPEN → HALF_OPEN)
 *   - Retry com backoff exponencial
 *   - Bulkhead (isolamento de concorrência)
 *   - Rate Limiter (controle de requisições)
 *   - Time Limiter (timeout reativo)
 *   - Spring Cloud Gateway (API Gateway)
 *   - Service Discovery com Eureka
 *   - Spring Cloud LoadBalancer
 *   - WebClient para comunicação entre serviços
 *   - Kafka (KafkaTemplate, @KafkaListener, Consumer Groups)
 *   - RabbitMQ (RabbitTemplate, @RabbitListener, DLQ)
 *
 * @EnableDiscoveryClient registra este serviço no Eureka Server.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EcommerceResilienceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceResilienceApplication.class, args);
    }
}
