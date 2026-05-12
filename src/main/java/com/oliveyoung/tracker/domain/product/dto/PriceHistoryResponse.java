package com.oliveyoung.tracker.domain.product.dto;

import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PriceHistoryResponse {

    private Integer currentPrice;
    private Integer originalPrice;
    private Integer discountRate;
    private Boolean isSale;
    private LocalDateTime recordedAt;

    public static PriceHistoryResponse from(PriceHistory priceHistory) {
        return PriceHistoryResponse.builder()
                .currentPrice(priceHistory.getCurrentPrice())
                .originalPrice(priceHistory.getOriginalPrice())
                .discountRate(priceHistory.getDiscountRate())
                .isSale(priceHistory.getIsSale())
                .recordedAt(priceHistory.getRecordedAt())
                .build();
    }

    public static List<PriceHistoryResponse> fromList(List<PriceHistory> histories) {
        return histories.stream()
                .map(PriceHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
