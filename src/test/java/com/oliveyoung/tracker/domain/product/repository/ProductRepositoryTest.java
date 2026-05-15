package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
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
}