package com.ecommerce.observability.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de Cache em Dois Níveis (L1 + L2).
 *
 * ARQUITETURA:
 *
 *   Requisição → @Cacheable
 *       ↓
 *   L1 Caffeine (local, ~100ns) — hit? retorna imediatamente
 *       ↓ miss
 *   L2 Redis (~1ms) — hit? popula L1 e retorna
 *       ↓ miss
 *   Banco de dados — popula L2 e L1, retorna
 *
 * QUANDO USAR CADA NÍVEL:
 *   L1 Caffeine: dados lidos com altíssima frequência, tolerantes a stale curto
 *   L2 Redis: dados compartilhados entre instâncias, TTL mais longo
 *
 * CUIDADOS:
 *   ❌ Nunca cache entidades JPA gerenciadas (LazyInitializationException fora de sessão)
 *   ✅ Cache apenas DTOs/Projections (objetos serializáveis e desacoplados)
 *   ✅ Configure TTL diferente por cache (categorias > produtos > usuários)
 *   ✅ Use @CacheEvict nas mutations (POST/PUT/DELETE)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // =========================================================================
    // L2 – REDIS (CacheManager principal)
    // =========================================================================

    /**
     * RedisCacheManager com TTL diferente por cache.
     *
     * Serialização Jackson:
     *   Usa GenericJackson2JsonRedisSerializer para serializar objetos como JSON.
     *   Inclui @class no JSON para desserialização polimórfica.
     *   Alternativa mais segura: configurar tipo explicitamente por cache.
     */
    @Bean
    @Primary // CacheManager padrão – usado quando não especificado em @Cacheable
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {

        // Configuração base (aplica a todos os caches sem configuração específica)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))             // TTL padrão: 10 minutos
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .disableCachingNullValues();                  // nunca armazena null

        // TTL específico por cache (sobrescreve o padrão)
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("produtos",   defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("categorias", defaultConfig.entryTtl(Duration.ofHours(1)));   // muda raramente
        cacheConfigs.put("usuarios",   defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("estoque",    defaultConfig.entryTtl(Duration.ofSeconds(30))); // muda frequentemente

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware() // sincroniza cache com transação Spring
                .build();
    }

    // =========================================================================
    // L1 – CAFFEINE (cache local ultra-rápido)
    // =========================================================================

    /**
     * CaffeineCacheManager como L1.
     * Para usar explicitamente: @Cacheable(cacheManager = "caffeineCacheManager")
     *
     * Caffeine spec:
     *   maximumSize: limite de entradas (eviction por LRU/LFU)
     *   expireAfterWrite: TTL após escrita
     *   recordStats: habilita métricas de hit/miss (integra com Micrometer)
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Configuração padrão para todos os caches Caffeine
        manager.setCaffeineSpec(
            com.github.benmanes.caffeine.cache.CaffeineSpec.parse(
                "maximumSize=500," +
                "expireAfterWrite=60s," +
                "recordStats"              // habilita métricas para Micrometer
            )
        );

        return manager;
    }

    // =========================================================================
    // ObjectMapper configurado para Redis
    // =========================================================================

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // suporte a LocalDate, LocalDateTime

        // Inclui informação de tipo para desserialização polimórfica
        // ATENÇÃO: desabilite se não usar polimorfismo – reduz tamanho do JSON
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return mapper;
    }
}
