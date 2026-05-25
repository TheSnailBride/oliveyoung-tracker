package com.oliveyoung.tracker.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
public class RequestTimingFilter extends OncePerRequestFilter {

    static final String RESPONSE_TIME_HEADER = "X-Response-Time-Ms";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            String elapsed = String.format(Locale.US, "%.2f", elapsedMs);
            response.setHeader(RESPONSE_TIME_HEADER, elapsed);
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
        }
    }
}
