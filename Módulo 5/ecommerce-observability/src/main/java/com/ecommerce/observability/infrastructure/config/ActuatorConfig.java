package com.ecommerce.observability.infrastructure.config;

import com.ecommerce.observability.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuração customizada do Spring Boot Actuator.
 *
 * Actuator expõe endpoints de observabilidade:
 *   /actuator/health     → status da aplicação e dependências
 *   /actuator/metrics    → métricas Micrometer
 *   /actuator/prometheus → métricas no formato Prometheus
 *   /actuator/info       → metadados da aplicação
 *   /actuator/env        → variáveis de ambiente
 *   /actuator/threaddump → dump das threads da JVM
 *   /actuator/heapdump   → dump da heap (para análise com VisualVM/MAT)
 *   /actuator/loggers    → leitura e alteração dinâmica de log levels
 *   /actuator/caches     → visualização e limpeza de caches
 *
 * SEGURANÇA: restrinja endpoints sensíveis (/env, /heapdump) via Spring Security.
 * Nunca exponha /actuator/* publicamente sem autenticação em produção.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ActuatorConfig {

    private final ProdutoRepository produtoRepository;
    private final RedisConnectionFactory redisConnectionFactory;

    // =========================================================================
    // HEALTH INDICATORS customizados
    // =========================================================================

    /**
     * HealthIndicator customizado para o banco de produtos.
     * Aparece em /actuator/health como "produtosDb".
     *
     * Status: UP, DOWN, OUT_OF_SERVICE, UNKNOWN
     * Spring agrega todos os indicadores – se qualquer um for DOWN, a app é DOWN.
     */
    @Bean
    public HealthIndicator produtosDbHealthIndicator() {
        return () -> {
            try {
                long total = produtoRepository.countByAtivoTrue();
                return Health.up()
                        .withDetail("produtos_ativos", total)
                        .withDetail("status", "Banco acessível")
                        .withDetail("verificado_em", LocalDateTime.now())
                        .build();
            } catch (Exception ex) {
                log.error("[HEALTH] Falha ao verificar banco de produtos: {}", ex.getMessage());
                return Health.down()
                        .withDetail("erro", ex.getMessage())
                        .withException(ex)
                        .build();
            }
        };
    }

    /**
     * HealthIndicator para o Redis.
     * Verifica se o Redis está acessível com um PING.
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                var connection = redisConnectionFactory.getConnection();
                String pong = connection.ping();
                connection.close();

                if ("PONG".equals(pong)) {
                    return Health.up()
                            .withDetail("redis", "Conectado")
                            .withDetail("resposta", pong)
                            .build();
                }
                return Health.unknown()
                        .withDetail("redis", "Resposta inesperada: " + pong)
                        .build();

            } catch (Exception ex) {
                return Health.down()
                        .withDetail("redis", "Indisponível")
                        .withDetail("erro", ex.getMessage())
                        .build();
            }
        };
    }

    // =========================================================================
    // INFO CONTRIBUTOR – enriquece o endpoint /actuator/info
    // =========================================================================

    /**
     * Adiciona informações de runtime ao /actuator/info.
     * Complementa as informações estáticas do application.yml.
     */
    @Bean
    public InfoContributor runtimeInfoContributor() {
        return builder -> {
            Runtime runtime = Runtime.getRuntime();

            Map<String, Object> jvm = new LinkedHashMap<>();
            jvm.put("processors", runtime.availableProcessors());
            jvm.put("heap_used_mb",  (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576);
            jvm.put("heap_max_mb",   runtime.maxMemory() / 1_048_576);
            jvm.put("heap_free_mb",  runtime.freeMemory() / 1_048_576);
            jvm.put("java_version",  System.getProperty("java.version"));

            Map<String, Object> app = new LinkedHashMap<>();
            app.put("iniciado_em",   LocalDateTime.now());
            app.put("ambiente",      System.getProperty("APP_ENV", "dev"));

            try {
                app.put("produtos_ativos", produtoRepository.countByAtivoTrue());
            } catch (Exception ignored) {}

            builder.withDetails(Map.of("jvm", jvm, "runtime", app));
        };
    }
}
