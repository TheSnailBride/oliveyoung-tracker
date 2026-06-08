package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductMaintenanceServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-26T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    @DisplayName("30일 이상 크롤링에서 보이지 않은 상품을 품절 처리한다")
    void markStaleProductsAsSoldOutUsesThirtyDayCutoff() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductMaintenanceService service = new ProductMaintenanceService(
                productRepository,
                FIXED_CLOCK
        );

        service.markStaleProductsAsSoldOut();

        verify(productRepository).markStaleProductsAsSoldOut(
                LocalDateTime.of(2026, 4, 26, 12, 0)
        );
    }
}
