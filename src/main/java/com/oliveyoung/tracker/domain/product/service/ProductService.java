package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.dto.PriceHistoryResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductDetailResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductResponse;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final long MIN_PRICE_HISTORY_COUNT_FOR_LOWEST = 2;
    private static final int HOME_VISIBLE_LAST_SEEN_DAYS = 2;

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Cacheable(value = "products", key = "#keyword + ':' + #category + ':' + #categories + ':' + #brand + ':' + #isSale + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ProductResponse> searchProducts(String keyword, String category,
                                                 List<String> categories,
                                                 String brand, Boolean isSale,
                                                 Pageable pageable) {
        if (category == null && categories != null && !categories.isEmpty()) {
            return productRepository.searchProductsByCategories(keyword, categories, brand, isSale, pageable)
                    .map(ProductResponse::from);
        }
        return productRepository.searchProducts(keyword, category, brand, isSale, pageable)
                .map(ProductResponse::from);
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        Integer lowestPrice = priceHistoryRepository.findLowestPriceByProductId(productId).orElse(null);
        Integer highestPrice = priceHistoryRepository.findHighestPriceByProductId(productId).orElse(null);

        return ProductDetailResponse.from(product, lowestPrice, highestPrice);
    }

    public List<PriceHistoryResponse> getPriceHistory(Long productId, int days) {
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDateTime to = LocalDateTime.now();

        return PriceHistoryResponse.fromList(
                priceHistoryRepository.findByProductIdAndRecordedAtBetweenOrderByRecordedAtAsc(productId, from, to)
        );
    }

    @Cacheable(value = "topDiscounted", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Page<ProductResponse> getTopDiscounted(Pageable pageable) {
        return productRepository.findTopDiscounted(homeVisibleSince(), pageable).map(ProductResponse::from);
    }

    @Cacheable(value = "priceDropped", key = "#categories + ':' + #size")
    public List<ProductResponse> getPriceDroppedProducts(List<String> categories, int size) {
        LocalDateTime seenSince = homeVisibleSince();
        List<Product> products = categories == null || categories.isEmpty()
                ? productRepository.findPriceDroppedProducts(seenSince, PageRequest.of(0, size))
                : productRepository.findPriceDroppedProductsByCategories(categories, seenSince, PageRequest.of(0, size));

        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        List<Product> similarProducts = productRepository.findTop5ByCategoryAndBrandAndIdNot(
                product.getCategory(),
                product.getBrand(),
                product.getId()
        );

        return similarProducts.stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Cacheable(value = "atLowest", key = "'meaningful-v1:' + #size")
    public List<ProductResponse> getAtLowestPrice(int size) {
        return productRepository.findAtLowestPrice(MIN_PRICE_HISTORY_COUNT_FOR_LOWEST, homeVisibleSince(), PageRequest.of(0, size))
                .stream().map(ProductResponse::from).toList();
    }

    @Cacheable(value = "stats", key = "'summary:v2'")
    public Map<String, Long> getStats() {
        LocalDateTime seenSince = homeVisibleSince();
        long total = productRepository.countByIsSoldOutFalseAndLastSeenAtGreaterThanEqual(seenSince);
        long onSale = productRepository.countByIsSaleTrueAndIsSoldOutFalseAndLastSeenAtGreaterThanEqual(seenSince);
        long atLowest = productRepository.countAtLowestPrice(MIN_PRICE_HISTORY_COUNT_FOR_LOWEST, seenSince);
        return Map.of("total", total, "onSale", onSale, "atLowest", atLowest);
    }

    public List<Map<String, String>> getAllProductsForCrawler() {
        return productRepository.findAllForCrawler();
    }

    private LocalDateTime homeVisibleSince() {
        return LocalDateTime.now().minusDays(HOME_VISIBLE_LAST_SEEN_DAYS);
    }

}
