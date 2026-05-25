package com.oliveyoung.tracker.common.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTimingFilterTest {

    @Test
    @DisplayName("API 응답에 서버 처리 시간 헤더를 추가한다")
    void addsResponseTimeHeaderToApiResponses() throws ServletException, IOException {
        RequestTimingFilter filter = new RequestTimingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Response-Time-Ms")).isNotBlank();
        assertThat(Double.parseDouble(response.getHeader("X-Response-Time-Ms"))).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("API가 아닌 응답에는 서버 처리 시간 헤더를 추가하지 않는다")
    void skipsNonApiResponses() throws ServletException, IOException {
        RequestTimingFilter filter = new RequestTimingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Response-Time-Ms")).isNull();
    }
}
