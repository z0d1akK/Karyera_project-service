package com.zodiakk.projectservice.config;

import com.zodiakk.projectservice.common.cache.CacheNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final String CACHE_PREFIX = "project-service:";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(20);

    private static final Duration DICTIONARY_TTL = Duration.ofMinutes(60);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        PolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build();

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(typeValidator)
                        .build();

        RedisSerializationContext.SerializationPair<Object> serializationPair =
                RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer);

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(DEFAULT_TTL)
                        .disableCachingNullValues()
                        .serializeValuesWith(serializationPair)
                        .prefixCacheNameWith(CACHE_PREFIX);

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                CacheNames.PROJECT_ROLES, createDictionaryConfiguration(serializationPair),
                CacheNames.TASK_TYPES, createDictionaryConfiguration(serializationPair)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache read failed for {}:{}", cache.getName(), key, ex);
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache write failed for {}:{}", cache.getName(), key, ex);
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache evict failed for {}:{}", cache.getName(), key, ex);
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache clear failed for {}", cache.getName(), ex);
            }
        };
    }

    private RedisCacheConfiguration createDictionaryConfiguration(
            RedisSerializationContext.SerializationPair<Object> serializationPair
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DICTIONARY_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(serializationPair)
                .prefixCacheNameWith(CACHE_PREFIX);
    }
}
