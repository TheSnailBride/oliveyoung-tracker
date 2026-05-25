package com.oliveyoung.tracker.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_histories", indexes = {
        @Index(name = "idx_product_recorded", columnList = "product_id, recordedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer currentPrice;

    private Integer originalPrice;
    private Integer discountRate;

    @Column(nullable = false)
    private Boolean isSale;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }

    public static PriceHistory of(Product product, Integer currentPrice, Integer originalPrice,
                                    Integer discountRate, Boolean isSale) {
        return PriceHistory.builder()
                .product(product)
                .currentPrice(currentPrice)
                .originalPrice(originalPrice)
                .discountRate(discountRate)
                .isSale(isSale != null ? isSale : false)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    public void updatePrice(Integer currentPrice, Integer originalPrice, Integer discountRate, Boolean isSale) {
        this.currentPrice = currentPrice;
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.isSale = isSale != null ? isSale : false;
        this.recordedAt = LocalDateTime.now();
    }
}
