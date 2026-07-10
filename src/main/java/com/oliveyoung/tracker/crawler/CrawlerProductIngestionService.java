package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.ProductCategory;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductCategoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrawlerProductIngestionService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional
    @CacheEvict(value = {"products", "topDiscounted", "priceDropped", "atLowest", "stats"}, allEntries = true)
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
    @CacheEvict(value = {"products", "topDiscounted", "priceDropped", "atLowest", "stats"}, allEntries = true)
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
                        .category(primaryCategory(crawled))
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
                syncCategories(product, crawled);

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
        product.markSeen();
        product.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                crawled.getDiscountRate(), crawled.getIsSale(), crawled.getIsSoldOut());
        if (updateInfo) {
            product.updateInfo(crawled.getName(), crawled.getBrand(), primaryCategory(crawled),
                    crawled.getImageUrl(), crawled.getProductUrl());
            syncCategories(product, crawled);
        }

        if (crawled.getCurrentPrice() == null) {
            return;
        }

        saveDailyLowestPriceHistory(product, crawled);

    }

    private String primaryCategory(CrawledProduct crawled) {
        if (crawled.getCategory() != null && !crawled.getCategory().isBlank()) {
            return crawled.getCategory();
        }
        return crawled.getCategories() == null
                ? null
                : crawled.getCategories().stream().filter(Objects::nonNull).map(String::trim)
                .filter(value -> !value.isBlank()).findFirst().orElse(null);
    }

    private void syncCategories(Product product, CrawledProduct crawled) {
        List<String> categories = crawled.getCategories() == null
                ? List.of()
                : crawled.getCategories().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        String primaryCategory = primaryCategory(crawled);
        if (primaryCategory != null && !categories.contains(primaryCategory)) {
            categories = java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(primaryCategory), categories.stream())
                    .distinct()
                    .toList();
        }

        for (String category : categories) {
            productCategoryRepository.findByProductIdAndCategoryName(product.getId(), category)
                    .ifPresentOrElse(ProductCategory::markSeen,
                            () -> productCategoryRepository.save(ProductCategory.of(product, category)));
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
