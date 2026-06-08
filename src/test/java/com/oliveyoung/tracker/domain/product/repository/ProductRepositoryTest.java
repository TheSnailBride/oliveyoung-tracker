package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Test
    @DisplayName("같은 브랜드와 카테고리를 가지며 특정 ID가 아닌 상품들을 제한된 개수로 조회한다")
    void findTop5ByCategoryAndBrandAndIdNot() {
        // given
        Product target = productRepository.save(Product.builder()
                .oliveYoungId("A001")
                .name("타겟 상품")
                .category("스킨케어")
                .brand("A브랜드")
                .isSale(false)
                .isSoldOut(false)
                .build());

        Product similar1 = productRepository.save(Product.builder()
                .oliveYoungId("A002")
                .name("유사 상품 1")
                .category("스킨케어")
                .brand("A브랜드")
                .isSale(false)
                .isSoldOut(false)
                .build());

        Product similar2 = productRepository.save(Product.builder()
                .oliveYoungId("A003")
                .name("다른 카테고리")
                .category("바디케어")
                .brand("A브랜드")
                .isSale(false)
                .isSoldOut(false)
                .build());

        // when
        List<Product> results = productRepository.findTop5ByCategoryAndBrandAndIdNot(
                "스킨케어", "A브랜드", target.getId());

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("유사 상품 1");
    }

    @Test
    @DisplayName("검색어 단어가 상품명에 모두 있으면 입력 순서와 중간 단어에 상관없이 조회한다")
    void searchProductsMatchesAllKeywordTermsRegardlessOfOrder() {
        saveProduct("SEARCH001", "아멜리 나르시시즘 립 틴트", "메이크업", 12_000, 15_000, 20, true, false);
        saveProduct("SEARCH002", "아멜리 아이섀도", "메이크업", 10_000, 12_000, 15, true, false);
        saveProduct("SEARCH003", "롬앤 쥬시 래스팅 틴트", "메이크업", 9_000, 13_000, 30, true, false);

        assertThat(productRepository.searchProducts("아멜리 틴트", null, null, null, PageRequest.of(0, 10)))
                .extracting(Product::getName)
                .containsExactly("아멜리 나르시시즘 립 틴트");

        assertThat(productRepository.searchProducts("틴트 아멜리", null, null, null, PageRequest.of(0, 10)))
                .extracting(Product::getName)
                .containsExactly("아멜리 나르시시즘 립 틴트");
    }

    @Test
    @DisplayName("역대 최저가 상품은 충분한 가격 이력이 있고 최고가 대비 하락률이 큰 순서로 조회한다")
    void findAtLowestPriceRequiresHistoryAndOrdersByDropRate() {
        Product bigDrop = saveProduct("LOW001", "하락률 큰 상품", 10_000, 20_000, 20, true, false);
        saveHistory(bigDrop, 20_000, 0);
        saveHistory(bigDrop, 10_000, 20);

        Product smallDropHighDiscount = saveProduct("LOW002", "할인율만 큰 상품", 24_000, 30_000, 40, true, false);
        saveHistory(smallDropHighDiscount, 30_000, 0);
        saveHistory(smallDropHighDiscount, 24_000, 40);

        Product insufficientHistory = saveProduct("LOW003", "이력 부족 상품", 5_000, 10_000, 50, true, false);
        saveHistory(insufficientHistory, 5_000, 50);

        Product notAtLowest = saveProduct("LOW004", "최저가 아닌 상품", 15_000, 20_000, 25, true, false);
        saveHistory(notAtLowest, 10_000, 50);
        saveHistory(notAtLowest, 15_000, 25);

        Product soldOut = saveProduct("LOW005", "품절 상품", 8_000, 16_000, 50, true, true);
        saveHistory(soldOut, 16_000, 0);
        saveHistory(soldOut, 8_000, 50);

        List<Product> results = productRepository.findAtLowestPrice(
                2,
                LocalDateTime.now().minusDays(2),
                PageRequest.of(0, 10)
        );

        assertThat(results)
                .extracting(Product::getName)
                .containsExactly("하락률 큰 상품", "할인율만 큰 상품");
    }

    @Test
    @DisplayName("카테고리 묶음 안에서 과거 가격보다 현재가가 내려간 상품을 하락률 순서로 조회한다")
    void findPriceDroppedProductsByCategoriesOrdersByDropRate() {
        LocalDateTime seenSince = LocalDateTime.now().minusDays(2);
        Product bigDrop = saveProduct("DROP001", "많이 하락한 상품", "스킨/토너", 10_000, 20_000, 20, true, false);
        saveHistory(bigDrop, 20_000, 0);
        saveHistory(bigDrop, 10_000, 20);

        Product smallDrop = saveProduct("DROP002", "조금 하락한 상품", "크림", 18_000, 20_000, 10, true, false);
        saveHistory(smallDrop, 20_000, 0);
        saveHistory(smallDrop, 18_000, 10);

        Product otherCategory = saveProduct("DROP003", "다른 카테고리 상품", "바디케어", 5_000, 10_000, 50, true, false);
        saveHistory(otherCategory, 10_000, 0);
        saveHistory(otherCategory, 5_000, 50);

        Product noDrop = saveProduct("DROP004", "하락 없는 상품", "스킨/토너", 20_000, 20_000, 0, false, false);
        saveHistory(noDrop, 20_000, 0);

        Product soldOut = saveProduct("DROP005", "품절 상품", "크림", 9_000, 20_000, 55, true, true);
        saveHistory(soldOut, 20_000, 0);
        saveHistory(soldOut, 9_000, 55);

        List<Product> results = productRepository.findPriceDroppedProductsByCategories(
                List.of("스킨/토너", "크림"),
                seenSince,
                PageRequest.of(0, 9)
        );

        assertThat(results)
                .extracting(Product::getName)
                .containsExactly("많이 하락한 상품", "조금 하락한 상품");
    }

    @Test
    @DisplayName("홈 노출 상품은 최근 2일 안에 다시 크롤링된 상품만 조회한다")
    void homeProductQueriesExcludeProductsNotSeenRecently() {
        LocalDateTime seenSince = LocalDateTime.now().minusDays(2);
        Product freshDrop = saveProduct("HOME001", "최근 확인 가격하락 상품", "스킨/토너", 10_000, 20_000, 20, true, false);
        saveHistory(freshDrop, 20_000, 0);
        saveHistory(freshDrop, 10_000, 20);

        Product staleDrop = saveProduct("HOME002", "오래 미확인 가격하락 상품", "스킨/토너", 9_000, 20_000, 55, true, false);
        markLastSeenAt(staleDrop, LocalDateTime.now().minusDays(3));
        saveHistory(staleDrop, 20_000, 0);
        saveHistory(staleDrop, 9_000, 55);

        List<Product> priceDrops = productRepository.findPriceDroppedProducts(seenSince, PageRequest.of(0, 9));
        List<Product> atLowest = productRepository.findAtLowestPrice(2, seenSince, PageRequest.of(0, 10));

        assertThat(priceDrops)
                .extracting(Product::getName)
                .contains("최근 확인 가격하락 상품")
                .doesNotContain("오래 미확인 가격하락 상품");
        assertThat(atLowest)
                .extracting(Product::getName)
                .contains("최근 확인 가격하락 상품")
                .doesNotContain("오래 미확인 가격하락 상품");
    }

    private Product saveProduct(String oliveYoungId, String name, Integer currentPrice,
                                Integer originalPrice, Integer discountRate,
                                boolean isSale, boolean isSoldOut) {
        return saveProduct(oliveYoungId, name, "스킨케어", currentPrice, originalPrice, discountRate, isSale, isSoldOut);
    }

    private Product saveProduct(String oliveYoungId, String name, String category, Integer currentPrice,
                                Integer originalPrice, Integer discountRate,
                                boolean isSale, boolean isSoldOut) {
        return productRepository.save(Product.builder()
                .oliveYoungId(oliveYoungId)
                .name(name)
                .category(category)
                .brand("테스트브랜드")
                .currentPrice(currentPrice)
                .originalPrice(originalPrice)
                .discountRate(discountRate)
                .isSale(isSale)
                .isSoldOut(isSoldOut)
                .build());
    }

    private void saveHistory(Product product, Integer currentPrice, Integer discountRate) {
        priceHistoryRepository.save(PriceHistory.of(product, currentPrice, product.getOriginalPrice(), discountRate, discountRate > 0));
    }

    private void markLastSeenAt(Product product, LocalDateTime lastSeenAt) {
        ReflectionTestUtils.setField(product, "lastSeenAt", lastSeenAt);
        productRepository.saveAndFlush(product);
    }
}
