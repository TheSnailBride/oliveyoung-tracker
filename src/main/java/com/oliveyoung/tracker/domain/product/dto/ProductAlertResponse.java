package com.oliveyoung.tracker.domain.product.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductAlertResponse {

    private Boolean isAlertSet;
    private Integer targetPrice;

    public static ProductAlertResponse set(Integer targetPrice) {
        return ProductAlertResponse.builder()
                .isAlertSet(true)
                .targetPrice(targetPrice)
                .build();
    }

    public static ProductAlertResponse cleared() {
        return ProductAlertResponse.builder()
                .isAlertSet(false)
                .targetPrice(null)
                .build();
    }
}
