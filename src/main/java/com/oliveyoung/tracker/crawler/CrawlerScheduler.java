package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import com.oliveyoung.tracker.domain.product.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NotificationService notificationService;

    @Value("${python.command:python}")
    private String pythonCmd;

    /**
     * 주기적으로 파이썬 크롤러 스크립트 실행 (6시간 간격 기본)
     */
    @Scheduled(fixedDelayString = "${crawler.interval:21600000}")
    public void runPythonScraper() {
        log.info("Starting Python Scraper automatically...");
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "scraper.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Scraper] " + line);
                }
            }

            int exitCode = process.waitFor();
            log.info("Python Scraper finished with exit code: " + exitCode);
        } catch (Exception e) {
            log.error("Error running Python Scraper: " + e.getMessage(), e);
        }
    }

    public void runManualCrawling() {
        runPythonScraper();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldPriceHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        priceHistoryRepository.deleteByRecordedAtBefore(cutoff);
        log.info("6개월 이상 된 가격 이력 삭제 완료 (기준: {})", cutoff);
    }

    @Transactional
    public int[] updatePricesOnly(List<CrawledProduct> crawledProducts) {
        int updatedCount = 0;
        for (CrawledProduct crawled : crawledProducts) {
            if (crawled.getOliveYoungId() == null || crawled.getOliveYoungId().isBlank()) continue;
            Product product = productRepository.findByOliveYoungId(crawled.getOliveYoungId()).orElse(null);
            if (product == null) continue;

            Integer oldPrice = product.getCurrentPrice();
            product.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                    crawled.getDiscountRate(), crawled.getIsSale(), crawled.getIsSoldOut());
            updatedCount++;

            if (crawled.getCurrentPrice() != null) {
                priceHistoryRepository.save(PriceHistory.of(product,
                        crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                        crawled.getDiscountRate(), crawled.getIsSale()));
                
                if (oldPrice != null && crawled.getCurrentPrice() < oldPrice) {
                    notificationService.checkAndSendPriceDropNotifications(product, oldPrice, crawled.getCurrentPrice());
                }
            }
        }
        return new int[]{updatedCount};
    }

    @Transactional
    public int[] saveCrawledProducts(List<CrawledProduct> crawledProducts) {
        int savedCount = 0, updatedCount = 0;

        List<String> oliveYoungIds = crawledProducts.stream()
                .map(CrawledProduct::getOliveYoungId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toList());

        Map<String, Product> existingProducts = productRepository.findByOliveYoungIdIn(oliveYoungIds).stream()
                .collect(Collectors.toMap(Product::getOliveYoungId, p -> p));

        for (CrawledProduct crawled : crawledProducts) {
            if (crawled.getOliveYoungId() == null || crawled.getOliveYoungId().isBlank()) continue;

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
                
                if (crawled.getCurrentPrice() != null) {
                    priceHistoryRepository.save(PriceHistory.of(product,
                            crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                            crawled.getDiscountRate(), crawled.getIsSale()));
                }
                savedCount++;
            } else {
                Integer oldPrice = product.getCurrentPrice();
                product.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                        crawled.getDiscountRate(), crawled.getIsSale(), crawled.getIsSoldOut());
                product.updateInfo(crawled.getName(), crawled.getBrand(), crawled.getImageUrl(), crawled.getProductUrl());
                updatedCount++;

                if (crawled.getCurrentPrice() != null) {
                    priceHistoryRepository.save(PriceHistory.of(product,
                            crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                            crawled.getDiscountRate(), crawled.getIsSale()));
                    
                    if (oldPrice != null && crawled.getCurrentPrice() < oldPrice) {
                        notificationService.checkAndSendPriceDropNotifications(product, oldPrice, crawled.getCurrentPrice());
                    }
                }
            }
        }
        return new int[]{savedCount, updatedCount};
    }
}
