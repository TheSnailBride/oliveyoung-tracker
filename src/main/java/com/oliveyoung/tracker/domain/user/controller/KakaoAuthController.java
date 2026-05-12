package com.oliveyoung.tracker.domain.user.controller;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.domain.user.service.KakaoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoService kakaoService;

    /**
     * 카카오 로그인 URL 반환
     */
    @GetMapping("/auth-url")
    public ApiResponse<Map<String, String>> getAuthUrl() {
        String url = kakaoService.getAuthorizationUrl();
        return ApiResponse.ok(Map.of("url", url));
    }

    /**
     * 카카오 OAuth 콜백
     * 로그인 성공 시 → /index.html?token=xxx&name=xxx 로 리다이렉트
     */
    @GetMapping("/callback")
    public void kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            response.sendRedirect("http://localhost:5173/login?error=kakao_denied");
            return;
        }

        try {
            String jwt = kakaoService.loginOrRegister(code);
            // 리액트 개발 서버로 리다이렉트하며 토큰 전달
            String redirectUrl = "http://localhost:5173/?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("카카오 로그인 실패 - type: {}, message: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            response.sendRedirect("http://localhost:5173/login?error=kakao_failed");
        }
    }
}
