package com.oliveyoung.tracker.config;

import com.oliveyoung.tracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${crawler.internal-token:}")
    private String crawlerInternalToken;

    @Value("${features.kakao-auth:false}")
    private boolean kakaoAuthEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/kakao/**").access(featureAccess(kakaoAuthEnabled))
                .requestMatchers(HttpMethod.POST, "/api/crawler/run").access(crawlerInternalAccess())
                .requestMatchers(HttpMethod.GET, "/api/products/all-for-crawler").access(crawlerInternalAccess())
                .requestMatchers("/api/crawler/**").access(crawlerInternalAccess())
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> featureAccess(boolean enabled) {
        return (authentication, context) -> new AuthorizationDecision(enabled);
    }

    private AuthorizationManager<RequestAuthorizationContext> crawlerInternalAccess() {
        return (authentication, context) -> {
            String providedToken = context.getRequest().getHeader("X-Crawler-Token");
            boolean granted = StringUtils.hasText(crawlerInternalToken)
                    && crawlerInternalToken.equals(providedToken);
            return new AuthorizationDecision(granted);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
