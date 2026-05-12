package com.oliveyoung.tracker.domain.user.repository;

import com.oliveyoung.tracker.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKakaoId(String kakaoId);

    boolean existsByEmail(String email);
}
