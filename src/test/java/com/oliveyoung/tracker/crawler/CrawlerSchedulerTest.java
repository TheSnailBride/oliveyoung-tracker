package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CrawlerSchedulerTest {

    @Autowired
    private CrawlerScheduler crawlerScheduler;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Test
    @DisplayName("같은 날 같은 상품의 가격 이력은 최저가 한 건만 유지하고 상품 현재가는 최신 가격으로 갱신한다")
    void saveCrawledProductsKeepsDailyLowestHistoryAndLatestProductPrice() {
        crawlerScheduler.saveCrawledProducts(List.of(product("A001", 10_000)));
        crawlerScheduler.saveCrawledProducts(List.of(product("A001", 8_000)));
        crawlerScheduler.saveCrawledProducts(List.of(product("A001", 9_000)));

        Product product = productRepository.findByOliveYoungId("A001").orElseThrow();
        List<PriceHistory> histories = priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(product.getId());

        assertThat(product.getCurrentPrice()).isEqualTo(9_000);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getCurrentPrice()).isEqualTo(8_000);
    }

    @Test
    @DisplayName("크롤러가 이미 실행 중이면 새 수동 실행 요청을 거절한다")
    void startManualCrawlingRejectsDuplicateRun() throws Exception {
        InMemoryCrawlerRunLock lock = new InMemoryCrawlerRunLock();
        BlockingCrawlerScheduler scheduler = new BlockingCrawlerScheduler(lock);

        assertThat(scheduler.startManualCrawling()).isTrue();
        assertThat(scheduler.awaitStarted()).isTrue();
        assertThat(scheduler.startManualCrawling()).isFalse();

        scheduler.finish();
        assertThat(scheduler.awaitFinished()).isTrue();
        assertThat(lock.awaitReleaseCount(1)).isTrue();
        assertThat(scheduler.startManualCrawling()).isTrue();
        assertThat(scheduler.awaitStarted()).isTrue();
        scheduler.finish();
        assertThat(scheduler.awaitFinished()).isTrue();
        assertThat(lock.awaitReleaseCount(2)).isTrue();
        assertThat(lock.releaseCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("서버 스케줄러는 락을 획득해 파이썬 크롤러를 자동 실행하고 종료 후 락을 해제한다")
    void scheduledCrawlingRunsWithLockAndReleasesIt() {
        InMemoryCrawlerRunLock lock = new InMemoryCrawlerRunLock();
        CountingCrawlerScheduler scheduler = new CountingCrawlerScheduler(lock);

        scheduler.runScheduledCrawling();

        assertThat(scheduler.executionCount()).isEqualTo(1);
        assertThat(lock.releaseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("파이썬 크롤러 프로세스에는 내부 API 토큰을 환경 변수로 전달한다")
    void crawlerProcessReceivesInternalTokenEnvironmentVariable() {
        ExposedProcessBuilderCrawlerScheduler scheduler = new ExposedProcessBuilderCrawlerScheduler(
                new InMemoryCrawlerRunLock()
        );
        ReflectionTestUtils.setField(scheduler, "crawlerInternalToken", "test-crawler-token");

        ProcessBuilder processBuilder = scheduler.exposeProcessBuilder();

        assertThat(processBuilder.environment().get("CRAWLER_INTERNAL_TOKEN"))
                .isEqualTo("test-crawler-token");
    }

    @Test
    @DisplayName("파이썬 크롤러 프로세스에는 서버 포트를 환경 변수로 전달한다")
    void crawlerProcessReceivesServerPortEnvironmentVariable() {
        ExposedProcessBuilderCrawlerScheduler scheduler = new ExposedProcessBuilderCrawlerScheduler(
                new InMemoryCrawlerRunLock()
        );
        ReflectionTestUtils.setField(scheduler, "serverPort", "18080");

        ProcessBuilder processBuilder = scheduler.exposeProcessBuilder();

        assertThat(processBuilder.environment().get("SERVER_PORT"))
                .isEqualTo("18080");
    }

    private CrawledProduct product(String oliveYoungId, int currentPrice) {
        return CrawledProduct.builder()
                .oliveYoungId(oliveYoungId)
                .name("테스트 상품")
                .brand("테스트 브랜드")
                .category("테스트 카테고리")
                .imageUrl("https://example.com/image.jpg")
                .productUrl("https://example.com/product/" + oliveYoungId)
                .currentPrice(currentPrice)
                .originalPrice(10_000)
                .discountRate(currentPrice < 10_000 ? 20 : 0)
                .isSale(currentPrice < 10_000)
                .isSoldOut(false)
                .build();
    }

    private static class BlockingCrawlerScheduler extends CrawlerScheduler {

        private CountDownLatch started = new CountDownLatch(1);
        private CountDownLatch finish = new CountDownLatch(1);
        private CountDownLatch finished = new CountDownLatch(1);

        BlockingCrawlerScheduler(CrawlerRunLock crawlerRunLock) {
            super(null, crawlerRunLock);
        }

        @Override
        protected void executePythonScraper() throws Exception {
            started.countDown();
            finish.await(5, TimeUnit.SECONDS);
            finished.countDown();
        }

        boolean awaitStarted() throws InterruptedException {
            return started.await(5, TimeUnit.SECONDS);
        }

        boolean awaitFinished() throws InterruptedException {
            boolean done = finished.await(5, TimeUnit.SECONDS);
            started = new CountDownLatch(1);
            finish = new CountDownLatch(1);
            finished = new CountDownLatch(1);
            return done;
        }

        void finish() {
            finish.countDown();
        }
    }

    private static class CountingCrawlerScheduler extends CrawlerScheduler {

        private int executionCount;

        CountingCrawlerScheduler(CrawlerRunLock crawlerRunLock) {
            super(null, crawlerRunLock);
        }

        @Override
        protected void executePythonScraper() {
            executionCount++;
        }

        int executionCount() {
            return executionCount;
        }
    }

    private static class ExposedProcessBuilderCrawlerScheduler extends CrawlerScheduler {

        ExposedProcessBuilderCrawlerScheduler(CrawlerRunLock crawlerRunLock) {
            super(null, crawlerRunLock);
        }

        ProcessBuilder exposeProcessBuilder() {
            return createProcessBuilder();
        }
    }

    private static class InMemoryCrawlerRunLock implements CrawlerRunLock {

        private final AtomicBoolean locked = new AtomicBoolean(false);
        private final AtomicInteger releaseCount = new AtomicInteger(0);

        @Override
        public Optional<Lease> tryAcquire() {
            if (!locked.compareAndSet(false, true)) {
                return Optional.empty();
            }
            return Optional.of(() -> {
                locked.set(false);
                releaseCount.incrementAndGet();
            });
        }

        int releaseCount() {
            return releaseCount.get();
        }

        boolean awaitReleaseCount(int expected) throws InterruptedException {
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
            while (System.currentTimeMillis() < deadline) {
                if (releaseCount.get() >= expected) {
                    return true;
                }
                Thread.sleep(10);
            }
            return false;
        }
    }
}
