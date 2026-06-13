package com.ecommerce.resilience.discovery;

/**
 * Configuração do Eureka Server.
 *
 * Em um projeto real, o Eureka Server seria uma aplicação Spring Boot SEPARADA.
 * Aqui documentamos como configurá-lo:
 *
 * ──────────────────────────────────────────────────────────────
 * Dependência (pom.xml do eureka-server):
 *   <dependency>
 *     <groupId>org.springframework.cloud</groupId>
 *     <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
 *   </dependency>
 *
 * ──────────────────────────────────────────────────────────────
 * Classe principal do eureka-server:
 *
 *   @SpringBootApplication
 *   @EnableEurekaServer
 *   public class EurekaServerApplication { ... }
 *
 * ──────────────────────────────────────────────────────────────
 * application.yml do eureka-server:
 *
 *   server:
 *     port: 8761
 *
 *   eureka:
 *     instance:
 *       hostname: localhost
 *     client:
 *       register-with-eureka: false   # o servidor não se registra nele mesmo
 *       fetch-registry: false
 *       service-url:
 *         defaultZone: http://localhost:8761/eureka/
 *     server:
 *       enable-self-preservation: false   # desativa em dev (evita falsos positivos)
 *       eviction-interval-timer-in-ms: 5000
 *
 * ──────────────────────────────────────────────────────────────
 * Como os microsserviços se registram (cada serviço):
 *
 *   @SpringBootApplication
 *   @EnableDiscoveryClient   # habilita o registro no Eureka
 *   public class OrderServiceApplication { ... }
 *
 *   # application.yml de cada microsserviço:
 *   spring.application.name: order-service
 *   eureka:
 *     client:
 *       service-url:
 *         defaultZone: http://localhost:8761/eureka/
 *
 * ──────────────────────────────────────────────────────────────
 * Como o @LoadBalanced WebClient resolve nomes:
 *
 *   // Esta URL:
 *   webClient.get().uri("http://estoque-service/api/v1/estoque")
 *
 *   // É resolvida pelo Spring Cloud LoadBalancer para:
 *   webClient.get().uri("http://192.168.1.10:8082/api/v1/estoque")
 *   // (endereço real da instância registrada no Eureka)
 *
 *   Algoritmos disponíveis:
 *     RoundRobinLoadBalancer (padrão)
 *     RandomLoadBalancer
 *     ServiceInstanceListSupplier customizado
 *
 * ──────────────────────────────────────────────────────────────
 * Dashboard Eureka:
 *   Acesse http://localhost:8761 para ver todas as instâncias registradas.
 */
public class EurekaServerDocumentation {
    // Classe apenas documentação – ver comentários acima
}
