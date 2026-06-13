package com.ecommerce.observability;

import com.ecommerce.observability.application.service.ProdutoService;
import com.ecommerce.observability.domain.model.Produto;
import com.ecommerce.observability.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Teste unitário do comportamento de cache.
 *
 * Usa @MockBean para isolar o repositório e verificar
 * quantas vezes o método real é chamado (uma vez com cache, N sem cache).
 *
 * Usa ConcurrentMapCacheManager em vez de Redis para testes unitários
 * – mais rápido, sem infraestrutura externa.
 */
@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "management.health.redis.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
@DisplayName("Testes Unitários de Cache – Anotações @Cacheable/@CacheEvict/@CachePut")
class CacheUnitTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean("redisCacheManager")
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                "produtos", "categorias", "usuarios", "estoque"
            );
        }
    }

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    ProdutoService produtoService;

    @MockBean
    ProdutoRepository produtoRepository;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void limparCache() {
        cacheManager.getCacheNames().forEach(name ->
            cacheManager.getCache(name).clear()
        );
    }

    // ── @Cacheable ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("@Cacheable: banco chamado apenas 1x para múltiplas buscas do mesmo ID")
    void cacheable_deveHitarCacheNaSegundaChamada() {
        Long id = 1L;
        Produto produto = Produto.builder().id(id).nome("Produto Cache").preco(BigDecimal.TEN).build();

        when(produtoRepository.findByIdWithCategoria(id)).thenReturn(Optional.of(produto));

        // Três chamadas ao serviço
        produtoService.buscarPorId(id);
        produtoService.buscarPorId(id);
        produtoService.buscarPorId(id);

        // Repositório deve ter sido chamado apenas 1x (as outras 2 vieram do cache)
        verify(produtoRepository, times(1)).findByIdWithCategoria(id);
    }

    @Test
    @DisplayName("@Cacheable: IDs diferentes não interferem entre si no cache")
    void cacheable_idsNaoInterfereNoCache() {
        Produto p1 = Produto.builder().id(1L).nome("Produto 1").preco(BigDecimal.TEN).build();
        Produto p2 = Produto.builder().id(2L).nome("Produto 2").preco(BigDecimal.ONE).build();

        when(produtoRepository.findByIdWithCategoria(1L)).thenReturn(Optional.of(p1));
        when(produtoRepository.findByIdWithCategoria(2L)).thenReturn(Optional.of(p2));

        produtoService.buscarPorId(1L);
        produtoService.buscarPorId(2L);
        produtoService.buscarPorId(1L); // do cache
        produtoService.buscarPorId(2L); // do cache

        // Cada ID acessa o banco apenas 1x
        verify(produtoRepository, times(1)).findByIdWithCategoria(1L);
        verify(produtoRepository, times(1)).findByIdWithCategoria(2L);
    }

    // ── @CacheEvict ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("@CacheEvict: após deletar, próxima busca vai ao banco")
    void cacheEvict_deveInvalidarCacheAoDeletar() {
        Long id = 10L;
        Produto produto = Produto.builder().id(id).nome("A deletar").preco(BigDecimal.TEN).build();

        when(produtoRepository.findByIdWithCategoria(id)).thenReturn(Optional.of(produto));
        doNothing().when(produtoRepository).deleteById(id);

        // 1ª busca – popula cache
        produtoService.buscarPorId(id);
        verify(produtoRepository, times(1)).findByIdWithCategoria(id);

        // Deleta – invalida o cache
        produtoService.deletar(id);

        // 2ª busca – cache invalidado, vai ao banco novamente
        produtoService.buscarPorId(id);
        verify(produtoRepository, times(2)).findByIdWithCategoria(id);
    }

    // ── @CachePut ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("@CachePut: após atualizar, próxima busca usa o valor atualizado do cache")
    void cachePut_deveSincronizarCacheComValorAtualizado() {
        Long id = 20L;
        Produto original   = Produto.builder().id(id).nome("Original")   .preco(BigDecimal.valueOf(100)).build();
        Produto atualizado = Produto.builder().id(id).nome("Atualizado") .preco(BigDecimal.valueOf(200)).build();

        when(produtoRepository.findByIdWithCategoria(id)).thenReturn(Optional.of(original));
        when(produtoRepository.save(atualizado)).thenReturn(atualizado);

        // Popula cache com o valor original
        Produto resultado1 = produtoService.buscarPorId(id);
        assertThat(resultado1.getNome()).isEqualTo("Original");

        // Atualiza (@CachePut sincroniza o cache com o valor novo)
        produtoService.atualizar(atualizado);

        // Próxima busca retorna o valor atualizado do CACHE (sem ir ao banco)
        Produto resultado2 = produtoService.buscarPorId(id);
        assertThat(resultado2.getNome()).isEqualTo("Atualizado");

        // findByIdWithCategoria só foi chamado 1x (a segunda busca veio do CachePut)
        verify(produtoRepository, times(1)).findByIdWithCategoria(id);
    }
}
