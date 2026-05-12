package com.oliveyoung.tracker.crawler.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawledProduct {

    private String oliveYoungId;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private String productUrl;
    private Integer currentPrice;
    private Integer originalPrice;
    private Integer discountRate;
    private Boolean isSale;
    private Boolean isSoldOut;
}
