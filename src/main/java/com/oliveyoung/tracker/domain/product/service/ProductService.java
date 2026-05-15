package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.dto.PriceHistoryResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductAlertRepository productAlertRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public Integer toggleAlert(String email, Long productId, Integer targetPrice) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Optional<ProductAlert> alertOpt = productAlertRepository.findByUserAndProduct(user, product);

        if (alertOpt.isPresent()) {
            ProductAlert alert = alertOpt.get();
            if (targetPrice == null) {
                // targetPrice가 안 넘어오면 알림 해제로 간주
                productAlertRepository.delete(alert);
                return null;
            } else {
                // 설정된 알림 업데이트
                alert.updateTargetPrice(targetPrice);
                return targetPrice;
            }
        } else {
            if (targetPrice == null) {
                 return null;
            }
            ProductAlert newAlert = ProductAlert.builder()
                    .user(user)
                    .product(product)
                    .targetPrice(targetPrice)
                    .build();
            productAlertRepository.save(newAlert);
            return targetPrice;
        }
    }

    public Map<String, Object> checkAlertStatus(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
                
        return productAlertRepository.findByUserAndProduct(user, product)
                .map(alert -> Map.of("isAlertSet", (Object) true, "targetPrice", alert.getTargetPrice() != null ? alert.getTargetPrice() : -1))
                .orElseGet(() -> Map.of("isAlertSet", false));
    }
}
