package com.oliveyoung.tracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("get", exception, cache, key);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logCacheError("put", exception, cache, key);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logCacheError("evict", exception, cache, key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed. cache={}, error={}", cache.getName(), exception.getMessage());
            }

            private void logCacheError(String operation, RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache {} failed. cache={}, key={}, error={}",
                        operation, cache.getName(), key, exception.getMessage());
            }
        };
    }
}
