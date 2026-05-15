package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.product.dto.PriceHistoryResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductDetailResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductResponse;
import com.oliveyoung.tracker.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 검색/필터
     * GET /api/products?keyword=토너&category=스킨케어&brand=라네즈&isSale=true&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isSale,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProductResponse> products = productService.searchProducts(keyword, category, brand, isSale, pageable);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    /**
     * 상품 상세 조회
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
        ProductDetailResponse product = productService.getProductDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    /**
     * 상품 가격 이력 조회 (차트용)
     * GET /api/products/{id}/prices?days=30
     */
    @GetMapping("/{id}/prices")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> getPriceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days) {

        List<PriceHistoryResponse> history = productService.getPriceHistory(id, days);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * 할인율 높은 상품 조회
     * GET /api/products/top-discounted
     */
    @GetMapping("/top-discounted")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getTopDiscounted(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductResponse> products = productService.getTopDiscounted(pageable);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{productId}/similar")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable Long productId) {
        List<ProductResponse> similarProducts = productService.getSimilarProducts(productId);
        return ResponseEntity.ok(ApiResponse.ok(similarProducts));
    }

    @GetMapping("/at-lowest")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAtLowest(
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAtLowestPrice(size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getStats()));
    }

    @GetMapping("/all-for-crawler")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAllForCrawler() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProductsForCrawler()));
    }

    @PostMapping("/{id}/alert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Integer> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Integer targetPrice = (body != null) ? body.get("targetPrice") : null;
        Integer setPrice = productService.toggleAlert(userDetails.getUsername(), id, targetPrice);
        
        boolean isAlertSet = (setPrice != null);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isAlertSet", isAlertSet, "targetPrice", setPrice != null ? setPrice : -1)));
    }

    @GetMapping("/{id}/alert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("isAlertSet", false, "targetPrice", -1)));
        }
        Map<String, Object> status = productService.checkAlertStatus(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
