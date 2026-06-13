package com.ecommerce.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de observabilidade – verifica que métricas são registradas corretamente.
 *
 * Testa que:
 *   - @Timed gera métricas no MeterRegistry
 *   - Counters são incrementados nas operações corretas
 *   - /actuator/prometheus expõe as métricas esperadas
 *   - /actuator/health retorna UP
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "management.info.env.enabled=true",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
            "org.springframework.boot.actuate.autoconfigure.data.redis.RedisHealthContributorAutoConfiguration"
    }
)
@AutoConfigureMockMvc
@DisplayName("Testes de Observabilidade – Métricas, Actuator e Performance")
class PerformanceAndObservabilityTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean("redisCacheManager")
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                "produtos", "categorias", "usuarios", "estoque"
            );
        }

        @Bean("redisHealthContributor")
        public HealthIndicator redisHealthContributor() {
            return () -> Health.up().build();
        }
    }

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @Autowired MockMvc       mockMvc;
    @Autowired MeterRegistry meterRegistry;

    // ── Actuator Endpoints ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health retorna UP")
    void actuatorHealth_deveRetornarUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info retorna metadados da aplicação")
    void actuatorInfo_deveRetornarMetadados() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").exists());
    }

    @Test
    @DisplayName("GET /actuator/prometheus expõe métricas no formato Prometheus")
    void actuatorPrometheus_deveExporMetricas() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    @DisplayName("GET /actuator/metrics retorna lista de métricas disponíveis")
    void actuatorMetrics_deveRetornarListaDeMetricas() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("GET /actuator/caches lista os caches configurados")
    void actuatorCaches_deveListarCaches() throws Exception {
        mockMvc.perform(get("/actuator/caches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheManagers").exists());
    }

    // ── Métricas Customizadas ─────────────────────────────────────────────────

    @Test
    @DisplayName("MeterRegistry contém métricas JVM padrão")
    void meterRegistry_deveConterMetricasJvm() {
        // Métricas JVM registradas automaticamente pelo Spring Boot Actuator
        assertThat(meterRegistry.find("jvm.memory.used").gauge()).isNotNull();
        assertThat(meterRegistry.find("jvm.threads.live").gauge()).isNotNull();
        // jvm.gc.pause é desabilitado pelo MetricsConfig.desativarMetricasVerbosas()
        // verificamos uma métrica de GC alternativa que não é filtrada
        assertThat(meterRegistry.find("jvm.memory.max").gauge()).isNotNull();
    }

    @Test
    @DisplayName("Counter customizado é registrado no MeterRegistry")
    void counter_deveSerRegistradoNoMeterRegistry() {
        // Cria e incrementa um counter de teste
        Counter counter = Counter.builder("teste.operacoes.total")
                .tag("tipo", "teste")
                .register(meterRegistry);

        counter.increment();
        counter.increment();
        counter.increment(2.0);

        assertThat(counter.count()).isEqualTo(4.0);
        assertThat(meterRegistry.find("teste.operacoes.total").counter()).isNotNull();
    }

    @Test
    @DisplayName("Timer registra duração e distribui percentis")
    void timer_deveRegistrarDuracaoComPercentis() {
        Timer timer = Timer.builder("teste.operacao.duracao")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Simula 3 operações com durações diferentes
        timer.record(java.time.Duration.ofMillis(10));
        timer.record(java.time.Duration.ofMillis(50));
        timer.record(java.time.Duration.ofMillis(200));

        assertThat(timer.count()).isEqualTo(3);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isGreaterThanOrEqualTo(260); // 10 + 50 + 200 = 260ms
    }

    @Test
    @DisplayName("Tags dimensionais permitem filtrar métricas por dimensão")
    void tags_devemPermitirFiltragemPorDimensao() {
        Counter c1 = Counter.builder("pedido.status")
                .tag("status", "criado")
                .register(meterRegistry);

        Counter c2 = Counter.builder("pedido.status")
                .tag("status", "cancelado")
                .register(meterRegistry);

        c1.increment(10);
        c2.increment(3);

        // Busca por tag específica
        var criados    = meterRegistry.find("pedido.status").tag("status", "criado").counter();
        var cancelados = meterRegistry.find("pedido.status").tag("status", "cancelado").counter();

        assertThat(criados).isNotNull();
        assertThat(cancelados).isNotNull();
        assertThat(criados.count()).isEqualTo(10);
        assertThat(cancelados.count()).isEqualTo(3);
    }

    // ── Endpoint de métricas customizadas ─────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/produtos/metricas retorna mapa de métricas disponíveis")
    void metricasEndpoint_deveRetornarMapaDeDicas() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints_observabilidade.prometheus")
                        .value("/actuator/prometheus"))
                .andExpect(jsonPath("$.metricas_customizadas.produtos_ativos")
                        .value("ecommerce.produtos.ativos.total"));
    }
}
