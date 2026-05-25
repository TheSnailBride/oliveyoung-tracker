package com.oliveyoung.tracker.domain.product.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.product.dto.NotificationCenterResponse;
import com.oliveyoung.tracker.domain.product.entity.Notification;
import com.oliveyoung.tracker.domain.product.repository.NotificationRepository;
import com.oliveyoung.tracker.domain.product.repository.ProductAlertRepository;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final ProductAlertRepository productAlertRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<NotificationCenterResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        NotificationCenterResponse responseData = NotificationCenterResponse.of(
                notificationRepository.findByUserOrderByCreatedAtDesc(user),
                productAlertRepository.findByUser(user)
        );

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @PostMapping("/read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Notification> unreadList = notificationRepository.findByUserAndIsReadFalse(user);
        for (Notification n : unreadList) {
            n.markAsRead();
        }

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{notificationId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        notificationRepository.delete(notification);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
