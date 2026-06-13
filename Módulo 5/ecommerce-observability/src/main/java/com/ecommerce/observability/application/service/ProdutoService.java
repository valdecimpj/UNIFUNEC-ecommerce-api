package com.ecommerce.observability.application.service;

import com.ecommerce.observability.domain.model.Produto;
import com.ecommerce.observability.domain.repository.ProdutoRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de produtos demonstrando:
 *   - Todas as anotações de cache (@Cacheable, @CacheEvict, @CachePut, @Caching)
 *   - Specification para consultas dinâmicas (type-safe, sem proliferar métodos)
 *   - Instrumentação com Micrometer (@Timed, Counter, Timer programático)
 */
@Service
@Slf4j
public class ProdutoService {

    private final ProdutoRepository repository;

    // Contadores e timers registrados programaticamente
    private final Counter cacheMissCounter;
    private final Counter cacheHitCounter;
    private final Counter produtoCriadoCounter;
    private final Timer buscaTimer;

    public ProdutoService(ProdutoRepository repository, MeterRegistry registry) {
        this.repository = repository;

        // ── Registra métricas customizadas no MeterRegistry ──────────────────
        // Tags dimensionais permitem filtrar e agrupar no Grafana/Prometheus

        this.cacheMissCounter = Counter.builder("cache.miss.custom")
                .description("Cache misses no serviço de produtos")
                .tag("servico", "produto")
                .register(registry);

        this.cacheHitCounter = Counter.builder("cache.hit.custom")
                .description("Cache hits no serviço de produtos")
                .tag("servico", "produto")
                .register(registry);

        this.produtoCriadoCounter = Counter.builder("produto.criado.total")
                .description("Total de produtos criados")
                .tag("origem", "api")
                .register(registry);

        this.buscaTimer = Timer.builder("produto.busca.duration")
                .description("Duração das buscas de produto")
                .tag("operacao", "buscar_por_id")
                .publishPercentiles(0.5, 0.95, 0.99) // p50, p95, p99
                .register(registry);
    }

    // =========================================================================
    // @Cacheable – armazena resultado na primeira chamada
    // =========================================================================

    /**
     * @Cacheable: na 1ª chamada, executa o método e armazena.
     * Nas próximas, retorna do cache sem executar o método.
     *
     * key: expressão SpEL – aqui usa o parâmetro id
     * condition: só cacheia se id > 0 (exemplo de condição)
     * unless: NÃO cacheia se o resultado for null
     */
    @Cacheable(
        value = "produtos",
        key   = "#id",
        condition = "#id > 0",
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    @Timed(value = "produto.busca.id", description = "Tempo de busca por ID", percentiles = {0.5, 0.95, 0.99})
    public Produto buscarPorId(Long id) {
        log.debug("[CACHE MISS] Buscando produto {} no banco", id);
        cacheMissCounter.increment();

        return buscaTimer.record(() ->
            repository.findByIdWithCategoria(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id))
        );
    }

    /**
     * Cache com SpEL mais complexo: usa campo do objeto retornado como chave.
     * cacheManager = "caffeineCacheManager": usa L1 ao invés do Redis padrão.
     */
    @Cacheable(
        value = "produtos",
        key   = "#root.methodName + ':' + #nome",
        cacheManager = "caffeineCacheManager"  // usa L1 Caffeine explicitamente
    )
    @Transactional(readOnly = true)
    public List<Produto> buscarPorNome(String nome) {
        log.debug("[L1 CACHE MISS] Buscando produtos por nome: {}", nome);
        return repository.findAll(
            Specification.where(nomeContem(nome)).and(apenasAtivos())
        );
    }

    // =========================================================================
    // @CachePut – atualiza o cache sem pular a execução do método
    // =========================================================================

    /**
     * @CachePut: SEMPRE executa o método E atualiza o cache.
     * Use em mutations que retornam o objeto atualizado.
     *
     * Diferença para @Cacheable: @Cacheable pula o método se houver cache.
     * @CachePut sempre executa.
     */
    @CachePut(value = "produtos", key = "#produto.id")
    @Transactional
    @Timed(value = "produto.atualizar", description = "Tempo de atualização")
    public Produto atualizar(Produto produto) {
        log.info("[CACHE PUT] Atualizando produto {} e sincronizando cache", produto.getId());
        return repository.save(produto);
    }

    // =========================================================================
    // @CacheEvict – invalida cache em mutations
    // =========================================================================

    /**
     * @CacheEvict: remove a entrada do cache.
     * afterReturn = true: só invalida APÓS o método completar com sucesso.
     * Se o método lançar exceção, o cache NÃO é invalidado.
     */
    @CacheEvict(value = "produtos", key = "#id", beforeInvocation = false)
    @Transactional
    public void deletar(Long id) {
        log.info("[CACHE EVICT] Deletando produto {} e invalidando cache", id);
        repository.deleteById(id);
    }

    /**
     * allEntries = true: invalida TODO o cache "produtos".
     * Útil quando uma operação pode afetar múltiplos registros.
     */
    @CacheEvict(value = "produtos", allEntries = true)
    @Transactional
    public void invalidarTodoCache() {
        log.info("[CACHE EVICT ALL] Invalidando todo o cache de produtos");
    }

    // =========================================================================
    // @Caching – combina múltiplas operações em um método
    // =========================================================================

    /**
     * @Caching: agrupa múltiplas anotações de cache em um único método.
     * Aqui: cria o produto E armazena em cache E invalida a listagem.
     */
    @Caching(
        put    = { @CachePut(value = "produtos", key = "#result.id") },
        evict  = { @CacheEvict(value = "produtos", key = "'listagem'") }
    )
    @Transactional
    @Timed(value = "produto.criar", description = "Tempo de criação de produto")
    public Produto criar(Produto produto) {
        Produto salvo = repository.save(produto);
        produtoCriadoCounter.increment();
        log.info("[CACHE] Produto criado id={} | cache atualizado", salvo.getId());
        return salvo;
    }

    // =========================================================================
    // SPECIFICATION – consultas dinâmicas type-safe
    // =========================================================================

    /**
     * Consulta dinâmica com Specification.
     * Cada filtro é um Specification independente que pode ser combinado.
     *
     * Vantagens:
     *   ✅ Type-safe (sem strings de JPQL)
     *   ✅ Composável: and(), or(), not()
     *   ✅ Filtros opcionais sem proliferar métodos no Repository
     *   ✅ Reutilizável entre diferentes consultas
     */
    @Transactional(readOnly = true)
    @Timed(value = "produto.busca.filtro", description = "Busca com filtros dinâmicos")
    public Page<Produto> buscarComFiltros(
            String nome,
            BigDecimal precoMin,
            BigDecimal precoMax,
            Boolean ativo,
            Long categoriaId,
            Pageable pageable) {

        // Compõe os filtros dinamicamente – só inclui os que foram fornecidos
        Specification<Produto> spec = Specification
                .where(nome       != null ? nomeContem(nome)               : null)
                .and  (precoMin   != null ? precoMinimo(precoMin)          : null)
                .and  (precoMax   != null ? precoMaximo(precoMax)          : null)
                .and  (ativo      != null ? statusAtivo(ativo)             : null)
                .and  (categoriaId != null ? porCategoria(categoriaId)     : null);

        return repository.findAll(spec, pageable);
    }

    // ── Specifications reutilizáveis ──────────────────────────────────────────

    private Specification<Produto> nomeContem(String nome) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    private Specification<Produto> precoMinimo(BigDecimal min) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("preco"), min);
    }

    private Specification<Produto> precoMaximo(BigDecimal max) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("preco"), max);
    }

    private Specification<Produto> statusAtivo(Boolean ativo) {
        return (root, query, cb) ->
            cb.equal(root.get("ativo"), ativo);
    }

    private Specification<Produto> porCategoria(Long categoriaId) {
        return (root, query, cb) ->
            cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    private Specification<Produto> apenasAtivos() {
        return (root, query, cb) -> cb.isTrue(root.get("ativo"));
    }

    // ── Projection ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProdutoResumo> listarResumo() {
        return repository.findAllResumo();
    }
}
