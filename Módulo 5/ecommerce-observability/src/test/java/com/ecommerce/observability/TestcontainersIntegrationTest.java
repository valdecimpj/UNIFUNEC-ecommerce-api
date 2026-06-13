package com.ecommerce.observability;

import com.ecommerce.observability.application.service.ProdutoService;
import com.ecommerce.observability.domain.model.Produto;
import com.ecommerce.observability.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de Integração com Testcontainers.
 *
 * Testcontainers sobe containers Docker reais durante os testes.
 * Elimina a necessidade de H2 (que não representa o comportamento real do PostgreSQL)
 * ou de serviços externos pré-instalados.
 *
 * Vantagens sobre H2 em memória:
 *   ✅ Testa comportamento real do PostgreSQL (tipos, índices, constraints)
 *   ✅ Redis real com comportamento de expiração, serialização e cluster
 *   ✅ Cada test suite tem ambiente limpo e isolado
 *   ✅ Reproduzível em CI/CD (GitHub Actions, Jenkins etc.)
 *
 * @Testcontainers: ativa o gerenciamento automático de containers
 * @Container: define o ciclo de vida do container (1 por classe = mais rápido)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Testes de Integração – Testcontainers (PostgreSQL + Redis)")
class TestcontainersIntegrationTest {

    // ── Containers ────────────────────────────────────────────────────────────

    /**
     * PostgreSQL container.
     * static = compartilhado entre TODOS os testes da classe (mais rápido).
     * Sem static = novo container por teste (mais isolado, mais lento).
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test")
            // withReuse(true): reutiliza o container entre execuções (requer ~/.testcontainers.properties)
            .withReuse(false);

    /**
     * Redis container.
     * GenericContainer é usado quando não há módulo específico do Testcontainers.
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(false);

    /**
     * DynamicPropertySource: injeta as propriedades dos containers no contexto Spring
     * ANTES de criar os beans. É o mecanismo correto para Testcontainers no Boot 3.
     *
     * Alternativa: @ServiceConnection (Boot 3.1+) que detecta automaticamente
     * o tipo do container e configura as propriedades.
     */
    @DynamicPropertySource
    static void configurarPropriedades(DynamicPropertyRegistry registry) {
        // Configura datasource com a URL real do container PostgreSQL
        registry.add("spring.datasource.url",         postgres::getJdbcUrl);
        registry.add("spring.datasource.username",     postgres::getUsername);
        registry.add("spring.datasource.password",     postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Configura Redis com a porta mapeada pelo container
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired ProdutoService   produtoService;
    @Autowired ProdutoRepository repository;
    @Autowired CacheManager      cacheManager;

    private Produto produtoSalvo;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        // Limpa o cache antes de cada teste
        cacheManager.getCacheNames().forEach(name ->
            cacheManager.getCache(name).clear()
        );

        produtoSalvo = produtoService.criar(Produto.builder()
                .nome("Notebook Dell XPS")
                .preco(BigDecimal.valueOf(7999.90))
                .estoque(5)
                .ativo(true)
                .build());
    }

    // ── Testes de CRUD com PostgreSQL real ────────────────────────────────────

    @Test
    @DisplayName("Cria produto e persiste no PostgreSQL real")
    void criar_devePersistitrNoPostgres() {
        assertThat(produtoSalvo.getId()).isNotNull();
        assertThat(produtoSalvo.getNome()).isEqualTo("Notebook Dell XPS");

        // Verifica direto no banco (sem passar pelo cache)
        assertThat(repository.findById(produtoSalvo.getId())).isPresent();
    }

    @Test
    @DisplayName("PostgreSQL: constraints são respeitadas (nome NOT NULL)")
    void criar_deveRespeitarConstraintNotnull() {
        Produto semNome = Produto.builder()
                .preco(BigDecimal.valueOf(100))
                .estoque(1)
                .build();

        // Em H2 às vezes passa, no PostgreSQL real as constraints são mais rígidas
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> repository.saveAndFlush(semNome)
        ).isInstanceOf(Exception.class);
    }

    // ── Testes de Cache com Redis real ────────────────────────────────────────

    @Test
    @DisplayName("@Cacheable: segunda chamada usa cache (Redis real)")
    void cacheable_segundaChamada_deveUsarRedis() {
        Long id = produtoSalvo.getId();

        // 1ª chamada: acessa o banco e popula o Redis
        Produto p1 = produtoService.buscarPorId(id);

        // Verifica que o valor está no Redis
        var cache = cacheManager.getCache("produtos");
        assertThat(cache).isNotNull();
        assertThat(cache.get(id)).isNotNull(); // entrada existe no Redis

        // 2ª chamada: deve vir do Redis (mesmo se banco for apagado)
        Produto p2 = produtoService.buscarPorId(id);

        assertThat(p1.getId()).isEqualTo(p2.getId());
        assertThat(p1.getNome()).isEqualTo(p2.getNome());
    }

    @Test
    @DisplayName("@CacheEvict: deletar remove a entrada do Redis")
    void cacheEvict_deveRemoverDoRedis() {
        Long id = produtoSalvo.getId();

        // Popula o cache
        produtoService.buscarPorId(id);
        assertThat(cacheManager.getCache("produtos").get(id)).isNotNull();

        // Deleta (aciona @CacheEvict)
        produtoService.deletar(id);

        // Entrada deve ter sido removida do Redis
        assertThat(cacheManager.getCache("produtos").get(id)).isNull();
    }

    @Test
    @DisplayName("@CachePut: atualizar sincroniza o Redis com o novo valor")
    void cachePut_deveAtualizarRedis() {
        Long id = produtoSalvo.getId();

        // Popula cache com valor original
        produtoService.buscarPorId(id);

        // Atualiza (aciona @CachePut)
        produtoSalvo.setNome("Notebook Dell XPS 15 ATUALIZADO");
        produtoService.atualizar(produtoSalvo);

        // O cache deve ter o valor atualizado
        var cached = cacheManager.getCache("produtos").get(id, Produto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.getNome()).isEqualTo("Notebook Dell XPS 15 ATUALIZADO");
    }

    @Test
    @DisplayName("Specification: busca com filtros dinâmicos no PostgreSQL")
    void specification_deveFiltraComMultiplosCriterios() {
        // Cria mais produtos para testar filtros
        produtoService.criar(Produto.builder().nome("Mouse Logitech")
                .preco(BigDecimal.valueOf(150)).estoque(20).ativo(true).build());
        produtoService.criar(Produto.builder().nome("Teclado Mecânico")
                .preco(BigDecimal.valueOf(400)).estoque(0).ativo(false).build());

        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        // Busca apenas ativos com preço acima de 100
        var resultado = produtoService.buscarComFiltros(
                null,
                BigDecimal.valueOf(100),
                null,
                true,
                null,
                pageable
        );

        assertThat(resultado.getContent()).isNotEmpty();
        assertThat(resultado.getContent())
                .allMatch(p -> p.getPreco().compareTo(BigDecimal.valueOf(100)) >= 0)
                .allMatch(Produto::isAtivo);
    }
}
