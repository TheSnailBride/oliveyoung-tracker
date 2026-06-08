package com.oliveyoung.tracker.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;  // 카카오 로그인 사용자는 null

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 카카오 연동 정보
    @Column(unique = true)
    private String kakaoId;

    @Column(columnDefinition = "TEXT")
    private String kakaoAccessToken;

    @Column(columnDefinition = "TEXT")
    private String kakaoRefreshToken;

    private LocalDateTime kakaoTokenExpiresAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) this.role = Role.USER;
    }

    public void updateKakaoToken(String kakaoId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.kakaoId = kakaoId;
        this.kakaoAccessToken = accessToken;
        this.kakaoRefreshToken = refreshToken;
        this.kakaoTokenExpiresAt = expiresAt;
    }

    public enum Role {
        USER, ADMIN
    }
}
