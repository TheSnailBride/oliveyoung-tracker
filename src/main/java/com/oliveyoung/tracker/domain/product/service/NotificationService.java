package com.oliveyoung.tracker.domain.product.service;

import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    /**
     * 가격 하락 알림 체크 및 발송
     * (이 기능은 알림 설정을 한 사용자들에게만 보내는 방식으로 확장 가능)
     */
    @Async
    @Transactional(readOnly = true)
    public void checkAndSendPriceDropNotifications(Product product, int oldPrice, int newPrice) {
        if (newPrice >= oldPrice) return;

        int dropAmount = oldPrice - newPrice;
        double dropRate = ((double) dropAmount / oldPrice) * 100;

        // 5% 이상 하락했을 때만 알림 (너무 잦은 알림 방지)
        if (dropRate < 5) return;

        String message = String.format("🔥 가격 하락 알림!\n\n[%s]\n%s\n\n이전 가격: %,d원\n현재 가격: %,d원 (▼%,d원, %.1f%% 하락)\n\n지금 바로 확인해보세요!",
                product.getBrand(), product.getName(), oldPrice, newPrice, dropAmount, dropRate);

        // 현재는 모든 사용자에게 테스트로 발송 (실제 서비스 시에는 찜한 사용자 등 조건 추가)
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.hasKakaoLinked()) {
                kakaoService.sendMessageToMe(user, message);
            }
        }
    }
}
