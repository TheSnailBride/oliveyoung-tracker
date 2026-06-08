package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByOliveYoungId(String oliveYoungId);

    List<Product> findByOliveYoungIdIn(List<String> oliveYoungIds);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByBrand(String brand, Pageable pageable);

    // 새롭게 추가되는 메서드
    List<Product> findTop5ByCategoryAndBrandAndIdNot(String category, String brand, Long id);

    default Page<Product> searchProducts(String keyword, String category, String brand, Boolean isSale, Pageable pageable) {
        return findAll(searchSpec(keyword, category, List.of(), brand, isSale), pageable);
    }

    default Page<Product> searchProductsByCategories(String keyword, List<String> categories, String brand, Boolean isSale, Pageable pageable) {
        return findAll(searchSpec(keyword, null, categories, brand, isSale), pageable);
    }

    private Specification<Product> searchSpec(String keyword, String category, List<String> categories, String brand, Boolean isSale) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            for (String term : keywordTerms(keyword)) {
                String pattern = "%" + escapeLike(term.toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("brand")), pattern, '\\')
                ));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").in(categories));
            }
            if (brand != null) {
                predicates.add(cb.equal(root.get("brand"), brand));
            }
            if (isSale != null) {
                predicates.add(cb.equal(root.get("isSale"), isSale));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private List<String> keywordTerms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return List.of(keyword.trim().split("\\s+")).stream()
                .filter(term -> !term.isBlank())
                .distinct()
                .toList();
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Query("""
            SELECT p FROM Product p
            WHERE p.isSoldOut = false
            AND p.lastSeenAt >= :seenSince
            ORDER BY p.discountRate DESC
            """)
    Page<Product> findTopDiscounted(@Param("seenSince") LocalDateTime seenSince, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM products p
            JOIN (
                SELECT product_id, MAX(current_price) AS max_price
                FROM price_histories
                GROUP BY product_id
            ) ph ON ph.product_id = p.id
            WHERE p.is_sold_out = false
            AND p.current_price IS NOT NULL
            AND p.last_seen_at >= :seenSince
            AND ph.max_price > p.current_price
            ORDER BY
                (ph.max_price - p.current_price) / ph.max_price DESC,
                p.discount_rate DESC,
                p.current_price ASC
            """, nativeQuery = true)
    List<Product> findPriceDroppedProducts(@Param("seenSince") LocalDateTime seenSince, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM products p
            JOIN (
                SELECT product_id, MAX(current_price) AS max_price
                FROM price_histories
                GROUP BY product_id
            ) ph ON ph.product_id = p.id
            WHERE p.is_sold_out = false
            AND p.current_price IS NOT NULL
            AND p.last_seen_at >= :seenSince
            AND p.category IN (:categories)
            AND ph.max_price > p.current_price
            ORDER BY
                (ph.max_price - p.current_price) / ph.max_price DESC,
                p.discount_rate DESC,
                p.current_price ASC
            """, nativeQuery = true)
    List<Product> findPriceDroppedProductsByCategories(
            @Param("categories") List<String> categories,
            @Param("seenSince") LocalDateTime seenSince,
            Pageable pageable
    );

    // 역대 최저가 도달 상품: 충분한 가격 이력이 있고, 현재가가 이력 최저가인 상품
    @Query("""
            SELECT p FROM Product p
            WHERE p.isSoldOut = false
            AND p.lastSeenAt >= :seenSince
            AND p.currentPrice IS NOT NULL
            AND p.currentPrice = (
                SELECT MIN(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id
            )
            AND (
                SELECT COUNT(ph) FROM PriceHistory ph WHERE ph.product.id = p.id
            ) >= :minHistoryCount
            AND p.discountRate > 0
            ORDER BY
                (
                    (
                        SELECT MAX(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id
                    ) - p.currentPrice
                ) * 1.0 / (
                    SELECT MAX(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id
                ) DESC,
                p.discountRate DESC,
                p.currentPrice ASC
            """)
    List<Product> findAtLowestPrice(
            @Param("minHistoryCount") long minHistoryCount,
            @Param("seenSince") LocalDateTime seenSince,
            Pageable pageable
    );

    long countByIsSoldOutFalseAndLastSeenAtGreaterThanEqual(LocalDateTime seenSince);

    long countByIsSaleTrueAndIsSoldOutFalseAndLastSeenAtGreaterThanEqual(LocalDateTime seenSince);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.isSoldOut = false
            AND p.lastSeenAt >= :seenSince
            AND p.currentPrice IS NOT NULL
            AND p.currentPrice = (
                SELECT MIN(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id
            )
            AND (
                SELECT COUNT(ph) FROM PriceHistory ph WHERE ph.product.id = p.id
            ) >= :minHistoryCount
            AND p.discountRate > 0
            """)
    long countAtLowestPrice(
            @Param("minHistoryCount") long minHistoryCount,
            @Param("seenSince") LocalDateTime seenSince
    );

    @Query("SELECT p.oliveYoungId as oliveYoungId, p.productUrl as productUrl FROM Product p")
    List<Map<String, String>> findAllForCrawler();

    @Modifying
    @Query("UPDATE Product p SET p.isSoldOut = true " +
           "WHERE (p.lastSeenAt IS NULL OR p.lastSeenAt < :cutoff) AND p.isSoldOut = false")
    int markStaleProductsAsSoldOut(@Param("cutoff") LocalDateTime cutoff);
}
