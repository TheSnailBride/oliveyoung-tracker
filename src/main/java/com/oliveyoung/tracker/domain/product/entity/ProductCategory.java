package com.oliveyoung.tracker.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_category_name",
                columnNames = {"product_id", "category_name"}
        ),
        indexes = @Index(name = "idx_product_category_name", columnList = "category_name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    private ProductCategory(Product product, String categoryName) {
        this.product = product;
        this.categoryName = categoryName;
        this.firstSeenAt = LocalDateTime.now();
        this.lastSeenAt = this.firstSeenAt;
    }

    public static ProductCategory of(Product product, String categoryName) {
        return new ProductCategory(product, categoryName);
    }

    public void markSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
}
