package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Notification;
import com.oliveyoung.tracker.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadFalse(User user);
    Optional<Notification> findByIdAndUser(Long id, User user);
}
