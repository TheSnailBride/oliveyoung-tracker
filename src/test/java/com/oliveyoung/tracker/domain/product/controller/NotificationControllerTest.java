package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.domain.product.dto.NotificationCenterResponse;
import com.oliveyoung.tracker.domain.product.entity.Notification;
import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.ProductAlert;
import com.oliveyoung.tracker.domain.product.repository.NotificationRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductAlertRepository;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @Test
    @DisplayName("알림센터 응답은 발생한 알림과 현재 설정된 목표가 알림을 명시적인 계약으로 반환한다")
    void getNotificationsReturnsTypedNotificationCenterResponse() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        ProductAlertRepository productAlertRepository = mock(ProductAlertRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationController controller = new NotificationController(
                notificationRepository,
                productAlertRepository,
                userRepository
        );

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .nickname("tester")
                .role(User.Role.USER)
                .build();
        Product product = Product.builder()
                .id(10L)
                .oliveYoungId("A001")
                .name("테스트 상품")
                .brand("테스트 브랜드")
                .imageUrl("https://example.com/product.jpg")
                .currentPrice(12_000)
                .isSale(false)
                .isSoldOut(false)
                .build();
        Notification notification = Notification.builder()
                .id(100L)
                .user(user)
                .product(product)
                .priceAtAlert(11_000)
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 5, 25, 9, 30))
                .build();
        ProductAlert activeAlert = ProductAlert.builder()
                .id(200L)
                .user(user)
                .product(product)
                .targetPrice(10_000)
                .createdAt(LocalDateTime.of(2026, 5, 24, 8, 0))
                .build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(notification));
        when(productAlertRepository.findByUser(user)).thenReturn(List.of(activeAlert));

        ResponseEntity<?> response = controller.getNotifications(userDetails);
        Object data = ((com.oliveyoung.tracker.common.response.ApiResponse<?>) response.getBody()).getData();

        assertThat(data).isInstanceOf(NotificationCenterResponse.class);
        NotificationCenterResponse center = (NotificationCenterResponse) data;
        assertThat(center.getNotifications()).hasSize(1);
        assertThat(center.getNotifications().get(0).getProductId()).isEqualTo(10L);
        assertThat(center.getActiveAlerts()).hasSize(1);
        assertThat(center.getActiveAlerts().get(0).getTargetPrice()).isEqualTo(10_000);
        assertThat(center.getActiveAlerts().get(0).getCurrentPrice()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("도착한 알림 삭제는 로그인 사용자의 알림만 삭제한다")
    void deleteNotificationDeletesOwnedNotification() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        ProductAlertRepository productAlertRepository = mock(ProductAlertRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationController controller = new NotificationController(
                notificationRepository,
                productAlertRepository,
                userRepository
        );

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .nickname("tester")
                .role(User.Role.USER)
                .build();
        Product product = Product.builder()
                .id(10L)
                .oliveYoungId("A001")
                .name("테스트 상품")
                .brand("테스트 브랜드")
                .isSale(false)
                .isSoldOut(false)
                .build();
        Notification notification = Notification.builder()
                .id(100L)
                .user(user)
                .product(product)
                .priceAtAlert(11_000)
                .isRead(true)
                .createdAt(LocalDateTime.of(2026, 5, 25, 9, 30))
                .build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@example.com")
                .password("unused")
                .authorities("ROLE_USER")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByIdAndUser(100L, user)).thenReturn(Optional.of(notification));

        controller.deleteNotification(100L, userDetails);

        verify(notificationRepository).delete(notification);
    }
}
