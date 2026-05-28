package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.dto.PriceHistoryResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductAlertResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductDetailResponse;
import com.oliveyoung.tracker.domain.product.dto.ProductResponse;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.ProductAlert;
import com.oliveyoung.tracker.domain.product.repository.PriceHistoryRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductAlertRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductRepository;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.repository.UserRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final long MIN_PRICE_HISTORY_COUNT_FOR_LOWEST = 2;

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductAlertRepository productAlertRepository;
    private final UserRepository userRepository;

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
        return productRepository.findTopDiscounted(pageable).map(ProductResponse::from);
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
        return productRepository.findAtLowestPrice(MIN_PRICE_HISTORY_COUNT_FOR_LOWEST, PageRequest.of(0, size))
                .stream().map(ProductResponse::from).toList();
    }

    @Cacheable(value = "stats", key = "'summary:v2'")
    public Map<String, Long> getStats() {
        long total = productRepository.count();
        long onSale = productRepository.countByIsSaleTrue();
        long atLowest = productRepository.countAtLowestPrice(MIN_PRICE_HISTORY_COUNT_FOR_LOWEST);
        return Map.of("total", total, "onSale", onSale, "atLowest", atLowest);
    }

    public List<Map<String, String>> getAllProductsForCrawler() {
        return productRepository.findAllForCrawler();
    }

    @Transactional
    public ProductAlertResponse toggleAlert(String email, Long productId, Integer targetPrice) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Optional<ProductAlert> alertOpt = productAlertRepository.findByUserAndProduct(user, product);

        if (alertOpt.isPresent()) {
            ProductAlert alert = alertOpt.get();
            if (targetPrice == null) {
                productAlertRepository.delete(alert);
                return ProductAlertResponse.cleared();
            } else {
                alert.updateTargetPrice(targetPrice);
                return ProductAlertResponse.set(targetPrice);
            }
        } else {
            if (targetPrice == null) {
                return ProductAlertResponse.cleared();
            }
            ProductAlert newAlert = ProductAlert.builder()
                    .user(user)
                    .product(product)
                    .targetPrice(targetPrice)
                    .build();
            productAlertRepository.save(newAlert);
            return ProductAlertResponse.set(targetPrice);
        }
    }

    public ProductAlertResponse checkAlertStatus(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        return productAlertRepository.findByUserAndProduct(user, product)
                .map(alert -> ProductAlertResponse.set(alert.getTargetPrice()))
                .orElseGet(ProductAlertResponse::cleared);
    }
}
