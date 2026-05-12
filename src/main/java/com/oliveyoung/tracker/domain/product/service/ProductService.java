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

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Cacheable(value = "products", key = "#keyword + ':' + #category + ':' + #brand + ':' + #isSale + ':' + #pageable.pageNumber")
    public Page<ProductResponse> searchProducts(String keyword, String category,
                                                 String brand, Boolean isSale,
                                                 Pageable pageable) {
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

    @Cacheable(value = "topDiscounted", key = "#pageable.pageNumber")
    public Page<ProductResponse> getTopDiscounted(Pageable pageable) {
        return productRepository.findTopDiscounted(pageable).map(ProductResponse::from);
    }

    public List<ProductResponse> getSimilarProducts(Long productId, String category, int size) {
        return productRepository.findSimilarProducts(category, productId, PageRequest.of(0, size))
                .stream().map(ProductResponse::from).toList();
    }

    public List<ProductResponse> getAtLowestPrice(int size) {
        return productRepository.findAtLowestPrice(PageRequest.of(0, size))
                .stream().map(ProductResponse::from).toList();
    }

    public Map<String, Long> getStats() {
        long total = productRepository.count();
        long onSale = productRepository.countByIsSaleTrue();
        long atLowest = productRepository.countAtLowestPrice();
        return Map.of("total", total, "onSale", onSale, "atLowest", atLowest);
    }

    public List<Map<String, String>> getAllProductsForCrawler() {
        return productRepository.findAllForCrawler();
    }
}
