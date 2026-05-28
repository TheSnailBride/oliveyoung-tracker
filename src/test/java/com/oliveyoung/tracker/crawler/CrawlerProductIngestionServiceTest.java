package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CrawlerProductIngestionServiceTest {

    @Autowired
    private CrawlerProductIngestionService ingestionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @BeforeEach
    void setUp() {
        priceHistoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 배치에 중복 oliveYoungId가 있어도 상품은 한 건만 저장하고 최신 가격으로 갱신한다")
    void saveCrawledProductsHandlesDuplicateOliveYoungIdsInSameBatch() {
        int[] result = ingestionService.saveCrawledProducts(List.of(
                product("A001", 10_000),
                product("A001", 9_000)
        ));

        List<Product> products = productRepository.findAll();
        Product saved = products.get(0);
        List<PriceHistory> histories = priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(saved.getId());

        assertThat(result).containsExactly(1, 1);
        assertThat(products).hasSize(1);
        assertThat(saved.getCurrentPrice()).isEqualTo(9_000);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getCurrentPrice()).isEqualTo(9_000);
    }

    @Test
    @DisplayName("가격만 갱신할 때도 같은 날 가격 이력은 최저가 한 건만 유지하고 상품 현재가는 최신 가격으로 갱신한다")
    void updatePricesOnlyKeepsDailyLowestHistoryAndLatestProductPrice() {
        ingestionService.saveCrawledProducts(List.of(product("A001", 10_000)));
        ingestionService.updatePricesOnly(List.of(product("A001", 8_000)));
        ingestionService.updatePricesOnly(List.of(product("A001", 9_000)));

        Product product = productRepository.findByOliveYoungId("A001").orElseThrow();
        List<PriceHistory> histories = priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(product.getId());

        assertThat(product.getCurrentPrice()).isEqualTo(9_000);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getCurrentPrice()).isEqualTo(8_000);
    }

    @Test
    @DisplayName("상품이 다시 크롤링되면 가격이 같아도 마지막 확인 시각을 갱신한다")
    void saveCrawledProductsRefreshesLastSeenAtEvenWhenPriceIsUnchanged() {
        ingestionService.saveCrawledProducts(List.of(product("A001", 10_000)));
        Product firstSeen = productRepository.findByOliveYoungId("A001").orElseThrow();

        ingestionService.saveCrawledProducts(List.of(product("A001", 10_000)));
        Product secondSeen = productRepository.findByOliveYoungId("A001").orElseThrow();

        assertThat(firstSeen.getLastSeenAt()).isNotNull();
        assertThat(secondSeen.getLastSeenAt()).isNotNull();
        assertThat(secondSeen.getLastSeenAt()).isAfterOrEqualTo(firstSeen.getLastSeenAt());
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
}
