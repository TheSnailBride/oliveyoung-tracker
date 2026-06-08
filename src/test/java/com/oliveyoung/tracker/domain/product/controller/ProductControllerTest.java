package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    @Test
    @DisplayName("상품 목록 조회 page size는 서버 상한으로 제한한다")
    void searchProductsCapsPageSize() {
        ProductService productService = mock(ProductService.class);
        ProductController controller = new ProductController(productService);
        when(productService.searchProducts(isNull(), isNull(), anyList(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.searchProducts(null, null, null, null, null, PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).searchProducts(isNull(), isNull(), anyList(), isNull(), isNull(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("가격 이력 조회 days는 서버 상한으로 제한한다")
    void getPriceHistoryCapsDays() {
        ProductService productService = mock(ProductService.class);
        ProductController controller = new ProductController(productService);
        when(productService.getPriceHistory(10L, 365)).thenReturn(List.of());

        controller.getPriceHistory(10L, 10_000);

        verify(productService).getPriceHistory(10L, 365);
    }

    @Test
    @DisplayName("역대 최저가 상품 size는 서버 상한으로 제한한다")
    void getAtLowestCapsSize() {
        ProductService productService = mock(ProductService.class);
        ProductController controller = new ProductController(productService);
        when(productService.getAtLowestPrice(50)).thenReturn(List.of());

        controller.getAtLowest(10_000);

        verify(productService).getAtLowestPrice(50);
    }

}
