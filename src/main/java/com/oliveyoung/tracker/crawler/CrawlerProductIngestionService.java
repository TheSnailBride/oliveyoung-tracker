package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import com.oliveyoung.tracker.domain.product.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlerProductIngestionService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NotificationService notificationService;

    @Transactional
    @CacheEvict(value = {"products", "topDiscounted", "atLowest", "stats"}, allEntries = true)
    public int[] updatePricesOnly(List<CrawledProduct> crawledProducts) {
        int updatedCount = 0;
        for (CrawledProduct crawled : crawledProducts) {
            if (!hasOliveYoungId(crawled)) continue;
            Product product = productRepository.findByOliveYoungId(crawled.getOliveYoungId()).orElse(null);
            if (product == null) continue;

            updateExistingProduct(product, crawled, false);
            updatedCount++;
        }
        return new int[]{updatedCount};
    }

    @Transactional
    @CacheEvict(value = {"products", "topDiscounted", "atLowest", "stats"}, allEntries = true)
    public int[] saveCrawledProducts(List<CrawledProduct> crawledProducts) {
        int savedCount = 0, updatedCount = 0;

        List<String> oliveYoungIds = crawledProducts.stream()
                .filter(this::hasOliveYoungId)
                .map(CrawledProduct::getOliveYoungId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Product> existingProducts = productRepository.findByOliveYoungIdIn(oliveYoungIds).stream()
                .collect(Collectors.toMap(Product::getOliveYoungId, product -> product));

        for (CrawledProduct crawled : crawledProducts) {
            if (!hasOliveYoungId(crawled)) continue;

            Product product = existingProducts.get(crawled.getOliveYoungId());

            if (product == null) {
                product = Product.builder()
                        .oliveYoungId(crawled.getOliveYoungId())
                        .name(crawled.getName())
                        .brand(crawled.getBrand())
                        .category(crawled.getCategory())
                        .imageUrl(crawled.getImageUrl())
                        .productUrl(crawled.getProductUrl())
                        .currentPrice(crawled.getCurrentPrice())
                        .originalPrice(crawled.getOriginalPrice())
                        .discountRate(crawled.getDiscountRate())
                        .isSale(crawled.getIsSale())
                        .isSoldOut(crawled.getIsSoldOut() != null ? crawled.getIsSoldOut() : false)
                        .build();
                productRepository.save(product);
                existingProducts.put(product.getOliveYoungId(), product);

                saveDailyLowestPriceHistory(product, crawled);
                savedCount++;
            } else {
                updateExistingProduct(product, crawled, true);
                updatedCount++;
            }
        }
        return new int[]{savedCount, updatedCount};
    }

    @Transactional
    public void deletePriceHistoryBefore(LocalDateTime cutoff) {
        priceHistoryRepository.deleteByRecordedAtBefore(cutoff);
    }

    private boolean hasOliveYoungId(CrawledProduct crawled) {
        return crawled.getOliveYoungId() != null && !crawled.getOliveYoungId().isBlank();
    }

    private void updateExistingProduct(Product product, CrawledProduct crawled, boolean updateInfo) {
        Integer oldPrice = product.getCurrentPrice();
        product.markSeen();
        product.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                crawled.getDiscountRate(), crawled.getIsSale(), crawled.getIsSoldOut());
        if (updateInfo) {
            product.updateInfo(crawled.getName(), crawled.getBrand(), crawled.getImageUrl(), crawled.getProductUrl());
        }

        if (crawled.getCurrentPrice() == null) {
            return;
        }

        saveDailyLowestPriceHistory(product, crawled);

        if (oldPrice != null && crawled.getCurrentPrice() < oldPrice) {
            notificationService.checkAndSendPriceDropNotifications(product, oldPrice, crawled.getCurrentPrice());
        }
    }

    private void saveDailyLowestPriceHistory(Product product, CrawledProduct crawled) {
        if (crawled.getCurrentPrice() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        priceHistoryRepository.findTopByProductIdAndRecordedAtBetweenOrderByCurrentPriceAscRecordedAtAsc(
                        product.getId(), startOfDay, startOfNextDay)
                .ifPresentOrElse(existing -> {
                    if (crawled.getCurrentPrice() < existing.getCurrentPrice()) {
                        existing.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                                crawled.getDiscountRate(), crawled.getIsSale());
                    }
                }, () -> priceHistoryRepository.save(PriceHistory.of(product,
                        crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                        crawled.getDiscountRate(), crawled.getIsSale())));
    }
}
