package com.oliveyoung.tracker.domain.product.dto;

import com.oliveyoung.tracker.domain.product.entity.Notification;
import com.oliveyoung.tracker.domain.product.entity.ProductAlert;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NotificationCenterResponse {

    private List<NotificationItem> notifications;
    private List<ActiveAlertItem> activeAlerts;

    public static NotificationCenterResponse of(List<Notification> notifications, List<ProductAlert> activeAlerts) {
        return NotificationCenterResponse.builder()
                .notifications(notifications.stream()
                        .map(NotificationItem::from)
                        .toList())
                .activeAlerts(activeAlerts.stream()
                        .map(ActiveAlertItem::from)
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class NotificationItem {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private Integer priceAtAlert;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static NotificationItem from(Notification notification) {
            return NotificationItem.builder()
                    .id(notification.getId())
                    .productId(notification.getProduct().getId())
                    .productName(notification.getProduct().getName())
                    .productImageUrl(notification.getProduct().getImageUrl() != null
                            ? notification.getProduct().getImageUrl()
                            : "")
                    .priceAtAlert(notification.getPriceAtAlert())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ActiveAlertItem {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
        private Integer targetPrice;
        private Integer currentPrice;
        private LocalDateTime createdAt;

        public static ActiveAlertItem from(ProductAlert alert) {
            return ActiveAlertItem.builder()
                    .id(alert.getId())
                    .productId(alert.getProduct().getId())
                    .productName(alert.getProduct().getName())
                    .productImageUrl(alert.getProduct().getImageUrl() != null
                            ? alert.getProduct().getImageUrl()
                            : "")
                    .targetPrice(alert.getTargetPrice())
                    .currentPrice(alert.getProduct().getCurrentPrice())
                    .createdAt(alert.getCreatedAt())
                    .build();
        }
    }
}
