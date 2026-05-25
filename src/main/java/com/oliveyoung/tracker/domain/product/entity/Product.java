package com.oliveyoung.tracker.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_olive_young_id", columnList = "oliveYoungId", unique = true),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_brand", columnList = "brand"),
        @Index(name = "idx_product_discount", columnList = "discountRate"),
        @Index(name = "idx_product_issale", columnList = "isSale")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String oliveYoungId;

    @Column(nullable = false)
    private String name;

    private String brand;
    private String category;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String productUrl;

    private Integer currentPrice;
    private Integer originalPrice;
    private Integer discountRate;

    @Column(nullable = false)
    private Boolean isSale = false;

    @Column(nullable = false)
    private Boolean isSoldOut = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceHistory> priceHistories = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updatePrice(Integer currentPrice, Integer originalPrice, Integer discountRate, Boolean isSale, Boolean isSoldOut) {
        if (currentPrice != null) this.currentPrice = currentPrice;
        if (originalPrice != null) this.originalPrice = originalPrice;
        if (discountRate != null) this.discountRate = discountRate;
        this.isSale = isSale;
        this.isSoldOut = isSoldOut;
    }

    public void updateInfo(String name, String brand, String imageUrl, String productUrl) {
        this.name = name;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.productUrl = productUrl;
    }
}
