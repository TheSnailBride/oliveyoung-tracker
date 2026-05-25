package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByOliveYoungId(String oliveYoungId);

    List<Product> findByOliveYoungIdIn(List<String> oliveYoungIds);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByBrand(String brand, Pageable pageable);

    // 새롭게 추가되는 메서드
    List<Product> findTop5ByCategoryAndBrandAndIdNot(String category, String brand, Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.brand LIKE %:keyword%) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:isSale IS NULL OR p.isSale = :isSale)")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("isSale") Boolean isSale,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.brand LIKE %:keyword%) AND " +
           "p.category IN :categories AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:isSale IS NULL OR p.isSale = :isSale)")
    Page<Product> searchProductsByCategories(
            @Param("keyword") String keyword,
            @Param("categories") List<String> categories,
            @Param("brand") String brand,
            @Param("isSale") Boolean isSale,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p ORDER BY p.discountRate DESC")
    Page<Product> findTopDiscounted(Pageable pageable);

    // 역대 최저가 도달 상품 (현재가 = 가격이력 최솟값)
    @Query("SELECT p FROM Product p WHERE p.currentPrice = " +
           "(SELECT MIN(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id) " +
           "AND p.discountRate > 0 ORDER BY p.discountRate DESC")
    List<Product> findAtLowestPrice(Pageable pageable);

    long countByIsSaleTrue();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.currentPrice = " +
           "(SELECT MIN(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = p.id) " +
           "AND p.discountRate > 0")
    long countAtLowestPrice();

    @Query("SELECT p.oliveYoungId as oliveYoungId, p.productUrl as productUrl FROM Product p")
    List<Map<String, String>> findAllForCrawler();

    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id != :excludeId ORDER BY p.discountRate DESC")
    List<Product> findSimilarProducts(@Param("category") String category, @Param("excludeId") Long excludeId, Pageable pageable);
}
