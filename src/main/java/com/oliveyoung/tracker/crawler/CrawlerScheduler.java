package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import com.oliveyoung.tracker.domain.product.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NotificationService notificationService;
    private final CrawlerRunLock crawlerRunLock;

    @Value("${python.command:python}")
    private String pythonCmd;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=================================================");
        log.info("CrawlerScheduler initialized!");
        log.info("Python command: {}", pythonCmd);
        log.info("Crawler auto schedule: disabled");
        log.info("=================================================");
    }

    /**
     * 파이썬 크롤러 스크립트 실행
     */
    private void runPythonScraper() {
        log.info("=================================================");
        log.info("Starting Python Scraper manually...");
        log.info("Command: {} -u scraper.py", pythonCmd);
        log.info("=================================================");
        try {
            executePythonScraper();
        } catch (Exception e) {
            log.error("Error running Python Scraper: " + e.getMessage(), e);
        }
    }

    protected void executePythonScraper() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-u", "scraper.py");
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
    }

    public boolean startManualCrawling() {
        Optional<CrawlerRunLock.Lease> lease = crawlerRunLock.tryAcquire();
        if (lease.isEmpty()) {
            log.warn("Python Scraper is already running. Ignoring duplicate manual request.");
            return false;
        }

        Thread crawlerThread = new Thread(() -> {
            try {
                runPythonScraper();
            } finally {
                lease.get().close();
            }
        }, "crawler-manual-runner");
        crawlerThread.start();
        return true;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldPriceHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        priceHistoryRepository.deleteByRecordedAtBefore(cutoff);
        log.info("6개월 이상 된 가격 이력 삭제 완료 (기준: {})", cutoff);
    }

    @Transactional
    @CacheEvict(value = {"products", "topDiscounted", "atLowest", "stats"}, allEntries = true)
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
                saveDailyLowestPriceHistory(product, crawled);

                if (oldPrice != null && crawled.getCurrentPrice() < oldPrice) {
                    notificationService.checkAndSendPriceDropNotifications(product, oldPrice, crawled.getCurrentPrice());
                }
            }
        }
        return new int[]{updatedCount};
    }

    @Transactional
    @CacheEvict(value = {"products", "topDiscounted", "atLowest", "stats"}, allEntries = true)
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
                
                saveDailyLowestPriceHistory(product, crawled);
                savedCount++;
            } else {
                Integer oldPrice = product.getCurrentPrice();
                product.updatePrice(crawled.getCurrentPrice(), crawled.getOriginalPrice(),
                        crawled.getDiscountRate(), crawled.getIsSale(), crawled.getIsSoldOut());
                product.updateInfo(crawled.getName(), crawled.getBrand(), crawled.getImageUrl(), crawled.getProductUrl());
                updatedCount++;

                if (crawled.getCurrentPrice() != null) {
                    saveDailyLowestPriceHistory(product, crawled);

                    if (oldPrice != null && crawled.getCurrentPrice() < oldPrice) {
                        notificationService.checkAndSendPriceDropNotifications(product, oldPrice, crawled.getCurrentPrice());
                    }
                }
            }
        }
        return new int[]{savedCount, updatedCount};
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
