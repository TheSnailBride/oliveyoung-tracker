package com.oliveyoung.tracker.domain.product.dto;

import com.oliveyoung.tracker.domain.product.entity.Product;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductDetailResponse {

    private Long id;
    private String oliveYoungId;
    private String name;
    private String brand;
    private String category;
    private List<String> categories;
    private String imageUrl;
    private String productUrl;
    private Integer currentPrice;
    private Integer originalPrice;
    private Integer discountRate;
    private Boolean isSale;
    private Integer lowestPrice;
    private Integer highestPrice;

    public static ProductDetailResponse from(Product product, Integer lowestPrice, Integer highestPrice) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .oliveYoungId(product.getOliveYoungId())
                .name(product.getName())
                .brand(product.getBrand())
                .category(product.getCategory())
                .categories(product.getCategoryNames())
                .imageUrl(product.getImageUrl())
                .productUrl(product.getProductUrl())
                .currentPrice(product.getCurrentPrice())
                .originalPrice(product.getOriginalPrice())
                .discountRate(product.getDiscountRate())
                .isSale(product.getIsSale())
                .lowestPrice(lowestPrice)
                .highestPrice(highestPrice)
                .build();
    }
}
