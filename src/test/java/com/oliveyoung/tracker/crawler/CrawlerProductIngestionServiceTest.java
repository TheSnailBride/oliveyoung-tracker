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

    @Test
    @DisplayName("기존 상품이 더모 카테고리에서 다시 발견되면 카테고리를 더모 카테고리로 갱신한다")
    void saveCrawledProductsUpdatesExistingProductCategory() {
        ingestionService.saveCrawledProducts(List.of(productWithCategory("A001", 10_000, "스킨/토너")));

        ingestionService.saveCrawledProducts(List.of(productWithCategory("A001", 9_000, "더모_스킨케어")));

        Product product = productRepository.findByOliveYoungId("A001").orElseThrow();
        assertThat(product.getCategory()).isEqualTo("더모_스킨케어");
    }

    @Test
    @DisplayName("기존 상품을 가격만 담긴 결과로 갱신해도 상품 기본 정보는 지우지 않는다")
    void saveCrawledProductsPreservesExistingInfoWhenCrawledInfoIsMissing() {
        ingestionService.saveCrawledProducts(List.of(productWithCategory("A001", 10_000, "더모_스킨케어")));

        ingestionService.saveCrawledProducts(List.of(CrawledProduct.builder()
                .oliveYoungId("A001")
                .productUrl("https://example.com/product/A001")
                .currentPrice(9_000)
                .originalPrice(10_000)
                .discountRate(10)
                .isSale(true)
                .isSoldOut(false)
                .build()));

        Product product = productRepository.findByOliveYoungId("A001").orElseThrow();
        assertThat(product.getName()).isEqualTo("테스트 상품");
        assertThat(product.getBrand()).isEqualTo("테스트 브랜드");
        assertThat(product.getCategory()).isEqualTo("더모_스킨케어");
        assertThat(product.getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    private CrawledProduct product(String oliveYoungId, int currentPrice) {
        return productWithCategory(oliveYoungId, currentPrice, "테스트 카테고리");
    }

    private CrawledProduct productWithCategory(String oliveYoungId, int currentPrice, String category) {
        return CrawledProduct.builder()
                .oliveYoungId(oliveYoungId)
                .name("테스트 상품")
                .brand("테스트 브랜드")
                .category(category)
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
