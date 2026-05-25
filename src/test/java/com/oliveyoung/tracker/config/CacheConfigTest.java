package com.oliveyoung.tracker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheConfigTest {

    @Test
    @DisplayName("캐시 서버 오류가 나도 API 처리를 막지 않도록 캐시 예외를 삼킨다")
    void cacheErrorsDoNotPropagate() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("products");
        CacheErrorHandler handler = new CacheConfig().errorHandler();
        RuntimeException exception = new RedisConnectionFailureException("redis unavailable");

        assertThatNoException().isThrownBy(() -> handler.handleCacheGetError(exception, cache, "key"));
        assertThatNoException().isThrownBy(() -> handler.handleCachePutError(exception, cache, "key", "value"));
        assertThatNoException().isThrownBy(() -> handler.handleCacheEvictError(exception, cache, "key"));
        assertThatNoException().isThrownBy(() -> handler.handleCacheClearError(exception, cache));
    }
}
