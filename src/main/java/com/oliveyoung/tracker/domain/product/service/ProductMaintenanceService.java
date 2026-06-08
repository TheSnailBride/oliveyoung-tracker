package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMaintenanceService {

    public static final int STALE_PRODUCT_HIDE_DAYS = 30;
    public static final int STALE_PRODUCT_DELETE_CANDIDATE_DAYS = 90;

    private final ProductRepository productRepository;
    private final Clock clock;

    @Transactional
    public void markStaleProductsAsSoldOut() {
        LocalDateTime cutoff = staleProductHideCutoff();
        int updatedCount = productRepository.markStaleProductsAsSoldOut(cutoff);
        log.info("Marked {} products as sold out because lastSeenAt is before {}", updatedCount, cutoff);
    }

    public LocalDateTime staleProductHideCutoff() {
        return LocalDateTime.now(clock).minusDays(STALE_PRODUCT_HIDE_DAYS);
    }

    public LocalDateTime staleProductDeleteCandidateCutoff() {
        return LocalDateTime.now(clock).minusDays(STALE_PRODUCT_DELETE_CANDIDATE_DAYS);
    }
}
