package com.ecommerce.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Módulo 5 – Performance, Observabilidade e Boas Práticas
 *
 * Pilares demonstrados:
 *   @EnableCaching   → habilita o proxy de cache para @Cacheable, @CacheEvict etc.
 *
 * Stack de observabilidade:
 *   Actuator   → /health, /metrics, /env, /threaddump, /heapdump
 *   Micrometer → facade de métricas (Counter, Gauge, Timer, @Timed)
 *   Prometheus → scrape via /actuator/prometheus
 *   Zipkin     → distributed tracing via Micrometer Tracing
 *
 * Cache em dois níveis:
 *   L1 → Caffeine (local, ~100ns de latência)
 *   L2 → Redis   (~1ms de latência)
 */
@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
public class EcommerceObservabilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceObservabilityApplication.class, args);
    }
}
