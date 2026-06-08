package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {ProductService.class, ProductServiceCacheTest.CacheTestConfig.class})
class ProductServiceCacheTest {

    @jakarta.annotation.Resource
    private ProductService productService;

    @jakarta.annotation.Resource
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 통계는 반복 조회 시 캐시를 사용한다")
    void getStatsUsesCacheForRepeatedCalls() {
        when(productRepository.countByIsSoldOutFalseAndLastSeenAtGreaterThanEqual(any())).thenReturn(100L);
        when(productRepository.countByIsSaleTrueAndIsSoldOutFalseAndLastSeenAtGreaterThanEqual(any())).thenReturn(30L);
        when(productRepository.countAtLowestPrice(eq(2L), any())).thenReturn(10L);

        Map<String, Long> first = productService.getStats();
        Map<String, Long> second = productService.getStats();

        assertThat(first).isEqualTo(second);
        verify(productRepository).countByIsSoldOutFalseAndLastSeenAtGreaterThanEqual(any());
        verify(productRepository).countByIsSaleTrueAndIsSoldOutFalseAndLastSeenAtGreaterThanEqual(any());
        verify(productRepository).countAtLowestPrice(eq(2L), any());
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("stats");
        }

        @Bean
        ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        PriceHistoryRepository priceHistoryRepository() {
            return mock(PriceHistoryRepository.class);
        }

    }
}
