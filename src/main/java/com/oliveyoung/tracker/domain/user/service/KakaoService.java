package com.oliveyoung.tracker.domain.user.service;

import com.oliveyoung.tracker.domain.user.dto.KakaoTokenResponse;
import com.oliveyoung.tracker.domain.user.dto.KakaoUserInfo;
import com.oliveyoung.tracker.domain.user.entity.User;
import com.oliveyoung.tracker.domain.user.repository.UserRepository;
import com.oliveyoung.tracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TOKEN_URL    = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String MESSAGE_URL  = "https://kapi.kakao.com/v2/api/talk/memo/default/send";
    private static final String AUTH_URL     = "https://kauth.kakao.com/oauth/authorize";

    /**
     * 카카오 로그인 URL 반환
     */
    public String getAuthorizationUrl() {
        return AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
    }

    /**
     * 카카오 로그인 처리
     */
    @Transactional
    public String loginOrRegister(String code) {
        log.info("카카오 로그인 처리 시작 - code: {}", code);

        // 1. 인가 코드로 카카오 토큰 발급 요청
        KakaoTokenResponse tokenResponse = exchangeCodeForToken(code);

        // 2. 발급받은 액세스 토큰으로 카카오 사용자 정보 조회
        KakaoUserInfo userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());

        // 3. 이메일 (또는 카카오 ID 기반 가상 이메일)로 가입 여부 확인
        String email = userInfo.getEmail() != null
                ? userInfo.getEmail()
                : userInfo.getKakaoId() + "@kakao.local";

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createKakaoUser(userInfo));

        // 4. 카카오 연동 정보 업데이트
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
        user.updateKakaoToken(
                userInfo.getKakaoId(),
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                expiresAt
        );

        log.info("카카오 로그인 성공 - email: {}, nickname: {}", user.getEmail(), user.getNickname());

        // 5. 자체 서비스용 JWT 발급
        return jwtTokenProvider.generateToken(user.getEmail());
    }

    private User createKakaoUser(KakaoUserInfo userInfo) {
        // 이메일이 없는 경우 kakaoId 기반으로 가상 이메일 생성
        String email = userInfo.getEmail() != null
                ? userInfo.getEmail()
                : userInfo.getKakaoId() + "@kakao.local";

        User user = User.builder()
                .email(email)
                .nickname(userInfo.getNickname())
                .role(User.Role.USER)
                .build();

        return userRepository.save(user);
    }

    private KakaoTokenResponse exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId.trim());
        params.add("redirect_uri", redirectUri.trim());
        params.add("code", code.trim());
        
        String secret = (clientSecret != null) ? clientSecret.trim() : "";
        boolean useSecret = !secret.isEmpty() && !secret.equals("YOUR_KAKAO_CLIENT_SECRET");
        
        if (useSecret) {
            params.add("client_secret", secret);
        }
        
        log.info("[카카오 토큰 요청 전송] URL: {}, clientId: {}, redirectUri: {}, useSecret: {}", 
                TOKEN_URL, clientId.trim(), redirectUri.trim(), useSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<KakaoTokenResponse> response =
                    restTemplate.postForEntity(TOKEN_URL, request, KakaoTokenResponse.class);
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("[카카오 API 에러] HTTP 상태코드: {}, 응답 본문: {}", e.getStatusCode(), errorBody);
            // 에러 메시지가 비어있다면 설정 문제일 확률 100%
            if (errorBody == null || errorBody.isBlank()) {
                log.error("!!! 카카오 응답 본문이 비어있습니다. 카카오 콘솔에서 '카카오 로그인 활성화(ON)' 및 'Client Secret' 설정을 다시 확인해 주세요 !!!");
            }
            throw e;
        }
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserInfo> response =
                restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, request, KakaoUserInfo.class);

        return response.getBody();
    }

    /**
     * 카카오 나에게 메시지 보내기
     */
    @Transactional
    public void sendMessageToMe(User user, String text) {
        if (!user.hasKakaoLinked()) {
            log.warn("카카오 미연동 유저 - userId: {}", user.getId());
            return;
        }

        String accessToken = getValidAccessToken(user);
        if (accessToken == null) {
            log.warn("카카오 토큰 갱신 실패 - userId: {}", user.getId());
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        String templateObject = "{\"object_type\":\"text\","
                + "\"text\":\"" + escapeJson(text) + "\","
                + "\"link\":{\"web_url\":\"https://www.oliveyoung.co.kr\","
                + "\"mobile_web_url\":\"https://www.oliveyoung.co.kr\"}}";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateObject);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            restTemplate.postForEntity(MESSAGE_URL, request, String.class);
            log.info("카카오 메시지 발송 완료 - userId: {}", user.getId());
        } catch (Exception e) {
            log.error("카카오 메시지 발송 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public String getValidAccessToken(User user) {
        if (!user.isKakaoTokenExpired()) {
            return user.getKakaoAccessToken();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("refresh_token", user.getKakaoRefreshToken());
            if (clientSecret != null && !clientSecret.isBlank()) {
                params.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<KakaoTokenResponse> response =
                    restTemplate.postForEntity(TOKEN_URL, request, KakaoTokenResponse.class);

            KakaoTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null) return null;

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
            String newRefresh = tokenResponse.getRefreshToken() != null
                    ? tokenResponse.getRefreshToken()
                    : user.getKakaoRefreshToken();

            user.updateKakaoToken(user.getKakaoId(), tokenResponse.getAccessToken(), newRefresh, expiresAt);
            return tokenResponse.getAccessToken();

        } catch (Exception e) {
            log.error("카카오 토큰 갱신 실패 - userId: {}, error: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n");
    }
}
