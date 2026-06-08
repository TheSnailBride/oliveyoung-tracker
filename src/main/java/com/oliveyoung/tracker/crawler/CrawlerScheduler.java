package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final CrawlerProductIngestionService ingestionService;
    private final CrawlerRunLock crawlerRunLock;

    @Value("${python.command:python}")
    private String pythonCmd;

    @Value("${crawler.internal-token:}")
    private String crawlerInternalToken;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${crawler.schedule.cron:0 0 3 * * *}")
    private String crawlerScheduleCron;

    @Value("${crawler.schedule.zone:Asia/Seoul}")
    private String crawlerScheduleZone;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=================================================");
        log.info("CrawlerScheduler initialized!");
        log.info("Python command: {}", pythonCmd);
        log.info("Crawler auto schedule: {} ({})", crawlerScheduleCron, crawlerScheduleZone);
        log.info("=================================================");
    }

    /**
     * 파이썬 크롤러 스크립트 실행
     */
    private void runPythonScraper(String trigger) {
        log.info("=================================================");
        log.info("Starting Python Scraper by {} trigger...", trigger);
        log.info("Command: {} -u scraper.py", pythonCmd);
        log.info("=================================================");
        try {
            executePythonScraper();
        } catch (Exception e) {
            log.error("Error running Python Scraper: " + e.getMessage(), e);
        }
    }

    protected void executePythonScraper() throws Exception {
        ProcessBuilder pb = createProcessBuilder();
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

    protected ProcessBuilder createProcessBuilder() {
        ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-u", "scraper.py");
        pb.environment().put("SERVER_PORT", StringUtils.hasText(serverPort) ? serverPort : "8080");
        if (StringUtils.hasText(crawlerInternalToken)) {
            pb.environment().put("CRAWLER_INTERNAL_TOKEN", crawlerInternalToken);
        }
        return pb;
    }

    public boolean startManualCrawling() {
        Optional<CrawlerRunLock.Lease> lease = crawlerRunLock.tryAcquire();
        if (lease.isEmpty()) {
            log.warn("Python Scraper is already running. Ignoring duplicate manual request.");
            return false;
        }

        Thread crawlerThread = new Thread(() -> runCrawlingWithLease("manual", lease.get()), "crawler-manual-runner");
        crawlerThread.start();
        return true;
    }

    @Scheduled(cron = "${crawler.schedule.cron:0 0 3 * * *}", zone = "${crawler.schedule.zone:Asia/Seoul}")
    public void runScheduledCrawling() {
        Optional<CrawlerRunLock.Lease> lease = crawlerRunLock.tryAcquire();
        if (lease.isEmpty()) {
            log.warn("Python Scraper is already running. Skipping scheduled request.");
            return;
        }

        runCrawlingWithLease("scheduled", lease.get());
    }

    private void runCrawlingWithLease(String trigger, CrawlerRunLock.Lease lease) {
        try {
            runPythonScraper(trigger);
        } finally {
            lease.close();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldPriceHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(6);
        ingestionService.deletePriceHistoryBefore(cutoff);
        log.info("6개월 이상 된 가격 이력 삭제 완료 (기준: {})", cutoff);
    }

    public int[] updatePricesOnly(List<CrawledProduct> crawledProducts) {
        return ingestionService.updatePricesOnly(crawledProducts);
    }

    public int[] saveCrawledProducts(List<CrawledProduct> crawledProducts) {
        return ingestionService.saveCrawledProducts(crawledProducts);
    }
}
