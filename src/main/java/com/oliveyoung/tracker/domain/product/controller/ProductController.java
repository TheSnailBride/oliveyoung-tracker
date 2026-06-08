package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.product.dto.PriceHistoryResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductDetailResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductResponse;
import com.oliveyoung.tracker.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PRICE_HISTORY_DAYS = 365;
    private static final int MAX_LOWEST_PRODUCTS_SIZE = 50;
    private static final int MAX_PRICE_DROPPED_PRODUCTS_SIZE = 30;

    private final ProductService productService;

    /**
     * 상품 목록 검색/필터
     * GET /api/products?keyword=토너&category=스킨케어&brand=라네즈&isSale=true&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categories,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isSale,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<String> categoryList = resolveCategoryList(null, categories);
        Page<ProductResponse> products = productService.searchProducts(keyword, category, categoryList, brand, isSale, capPageable(pageable));
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/price-drops")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPriceDrops(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categories,
            @RequestParam(defaultValue = "9") int size) {
        List<String> categoryList = resolveCategoryList(category, categories);
        return ResponseEntity.ok(ApiResponse.ok(productService.getPriceDroppedProducts(
                categoryList,
                cap(size, 1, MAX_PRICE_DROPPED_PRODUCTS_SIZE)
        )));
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

        List<PriceHistoryResponse> history = productService.getPriceHistory(id, cap(days, 1, MAX_PRICE_HISTORY_DAYS));
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * 할인율 높은 상품 조회
     * GET /api/products/top-discounted
     */
    @GetMapping("/top-discounted")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getTopDiscounted(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductResponse> products = productService.getTopDiscounted(capPageable(pageable));
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
        return ResponseEntity.ok(ApiResponse.ok(productService.getAtLowestPrice(cap(size, 1, MAX_LOWEST_PRODUCTS_SIZE))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getStats()));
    }

    @GetMapping("/all-for-crawler")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAllForCrawler() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProductsForCrawler()));
    }

    private Pageable capPageable(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    private int cap(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private List<String> resolveCategoryList(String category, String categories) {
        if (category != null && !category.isBlank()) {
            return List.of(category.trim());
        }

        return categories == null || categories.isBlank()
                ? List.of()
                : Arrays.stream(categories.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();
    }
}
