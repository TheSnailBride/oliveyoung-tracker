package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductAlertRequest;
import com.oliveyoung.tracker.domain.product.dto.ProductAlertResponse;
import com.oliveyoung.tracker.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAlertControllerTest {

    @Test
    @DisplayName("목표가 알림 설정 응답은 명시적인 DTO 계약으로 반환한다")
    void toggleAlertReturnsTypedProductAlertResponse() {
        ProductService productService = mock(ProductService.class);
        ProductController controller = new ProductController(productService);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();

        when(productService.toggleAlert("user@example.com", 10L, 12_000))
                .thenReturn(ProductAlertResponse.set(12_000));

        ResponseEntity<ApiResponse<ProductAlertResponse>> response = controller.toggleAlert(
                10L,
                new ProductAlertRequest(12_000),
                userDetails
        );

        ProductAlertResponse data = response.getBody().getData();
        assertThat(data.getIsAlertSet()).isTrue();
        assertThat(data.getTargetPrice()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("로그인하지 않은 상품의 목표가 알림 상태는 기본 DTO로 반환한다")
    void checkAlertReturnsDefaultTypedResponseForAnonymousUser() {
        ProductController controller = new ProductController(mock(ProductService.class));

        ResponseEntity<ApiResponse<ProductAlertResponse>> response = controller.checkAlert(10L, null);

        ProductAlertResponse data = response.getBody().getData();
        assertThat(data.getIsAlertSet()).isFalse();
        assertThat(data.getTargetPrice()).isNull();
    }
}
