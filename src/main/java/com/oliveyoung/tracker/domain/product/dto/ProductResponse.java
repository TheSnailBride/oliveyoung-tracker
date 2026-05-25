package com.oliveyoung.tracker.domain.product.dto;

import com.oliveyoung.tracker.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private Integer currentPrice;
    private Integer originalPrice;
    private Integer discountRate;
    private Boolean isSale;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .currentPrice(product.getCurrentPrice())
                .originalPrice(product.getOriginalPrice())
                .discountRate(product.getDiscountRate())
                .isSale(product.getIsSale())
                .build();
    }
}
