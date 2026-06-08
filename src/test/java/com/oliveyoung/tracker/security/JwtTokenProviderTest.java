package com.oliveyoung.tracker.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    @DisplayName("JWT secret이 비어 있으면 애플리케이션 시작 시 실패한다")
    void blankJwtSecretFailsFast() {
        assertThatThrownBy(() -> new JwtTokenProvider("", 86_400_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("공개 예시 JWT secret은 운영 기본값으로 사용할 수 없다")
    void documentedExampleJwtSecretFailsFast() {
        assertThatThrownBy(() -> new JwtTokenProvider(
                "your-secret-key-must-be-at-least-256-bits-long-for-hs256",
                86_400_000
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
