package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.entity.Notification;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.ProductAlert;
import com.oliveyoung.tracker.domain.product.repository.NotificationRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductAlertRepository;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.service.KakaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KakaoService kakaoService;
    private final ProductAlertRepository productAlertRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 가격 하락 알림 체크 및 발송
     * 사용자가 설정한 목표가에 도달한 경우에만 알림 발송 및 DB 저장
     */
    @Async
    @Transactional
    public void checkAndSendPriceDropNotifications(Product product, int oldPrice, int newPrice) {
        if (newPrice >= oldPrice) return;

        // 해당 상품에 알림을 설정한 모든 ProductAlert 조회
        List<ProductAlert> alerts = productAlertRepository.findByProduct(product);

        for (ProductAlert alert : alerts) {
            Integer targetPrice = alert.getTargetPrice();
            
            // 목표가가 설정되어 있고, 현재 가격이 목표가 이하인 경우 알림 발송
            if (targetPrice != null && newPrice <= targetPrice) {
                User user = alert.getUser();
                
                String message = String.format("🎉 목표가 도달 알림!\n\n[%s]\n%s\n\n설정한 목표가: %,d원\n현재 가격: %,d원\n\n지금 바로 확인해보세요!",
                        product.getBrand(), product.getName(), targetPrice, newPrice);

                // DB에 알림 내역 저장
                Notification notification = Notification.builder()
                        .user(user)
                        .product(product)
                        .priceAtAlert(newPrice)
                        .isRead(false)
                        .build();
                notificationRepository.save(notification);

                // 카카오톡 발송 (연동된 사용자만)
                if (user.hasKakaoLinked()) {
                    kakaoService.sendMessageToMe(user, message);
                }
                
                // 알림 발송 후 1회성 알림을 위해 설정 삭제 (유지하려면 이 줄을 제거)
                productAlertRepository.delete(alert);
            }
        }
    }
}
