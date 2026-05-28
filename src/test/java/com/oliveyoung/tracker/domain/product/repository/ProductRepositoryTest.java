package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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

        List<Product> results = productRepository.findAtLowestPrice(2, PageRequest.of(0, 10));

        assertThat(results)
                .extracting(Product::getName)
                .containsExactly("하락률 큰 상품", "할인율만 큰 상품");
    }

    private Product saveProduct(String oliveYoungId, String name, Integer currentPrice,
                                Integer originalPrice, Integer discountRate,
                                boolean isSale, boolean isSoldOut) {
        return productRepository.save(Product.builder()
                .oliveYoungId(oliveYoungId)
                .name(name)
                .category("스킨케어")
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
}
