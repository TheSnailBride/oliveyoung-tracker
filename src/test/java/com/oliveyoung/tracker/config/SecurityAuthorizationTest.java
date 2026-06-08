package com.oliveyoung.tracker.config;

import com.oliveyoung.tracker.crawler.CrawlerScheduler;
import com.oliveyoung.tracker.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "crawler.internal-token=test-crawler-token")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CrawlerScheduler crawlerScheduler;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("익명 사용자는 공개 상품 통계 조회가 가능하다")
    void anonymousUserCanReadPublicProductStats() throws Exception {
        when(productService.getStats()).thenReturn(Map.of(
                "total", 0L,
                "onSale", 0L,
                "atLowest", 0L
        ));

        mockMvc.perform(get("/api/products/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("응답에는 클릭재킹 방어를 위한 frame options 헤더가 포함된다")
    void responsesIncludeFrameOptionsHeader() throws Exception {
        when(productService.getStats()).thenReturn(Map.of(
                "total", 0L,
                "onSale", 0L,
                "atLowest", 0L
        ));

        mockMvc.perform(get("/api/products/stats"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    @DisplayName("배포 브랜치 기본 설정에서는 카카오 로그인 URL을 열지 않는다")
    void kakaoAuthUrlIsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/kakao/auth-url"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내부 토큰이 없는 요청은 크롤러 수동 실행을 요청할 수 없다")
    void requestWithoutInternalTokenCannotRunCrawler() throws Exception {
        when(crawlerScheduler.startManualCrawling()).thenReturn(true);

        mockMvc.perform(post("/api/crawler/run"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내부 토큰을 가진 서버 작업은 크롤러 수동 실행을 요청할 수 있다")
    void requestWithInternalTokenCanRunCrawler() throws Exception {
        when(crawlerScheduler.startManualCrawling()).thenReturn(true);

        mockMvc.perform(post("/api/crawler/run")
                        .header("X-Crawler-Token", "test-crawler-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내부 토큰이 없는 요청은 크롤러 데이터 임포트를 요청할 수 없다")
    void requestWithoutInternalTokenCannotImportCrawlerData() throws Exception {
        mockMvc.perform(post("/api/crawler/import")
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내부 토큰이 없는 요청은 크롤러용 전체 상품 목록을 조회할 수 없다")
    void requestWithoutInternalTokenCannotReadCrawlerCatalog() throws Exception {
        mockMvc.perform(get("/api/products/all-for-crawler"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내부 토큰을 가진 서버 작업은 크롤러 데이터 임포트를 요청할 수 있다")
    void requestWithInternalTokenCanImportCrawlerData() throws Exception {
        when(crawlerScheduler.saveCrawledProducts(anyList())).thenReturn(new int[]{0, 0});

        mockMvc.perform(post("/api/crawler/import")
                        .header("X-Crawler-Token", "test-crawler-token")
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내부 토큰을 가진 서버 작업은 크롤러용 전체 상품 목록을 조회할 수 있다")
    void requestWithInternalTokenCanReadCrawlerCatalog() throws Exception {
        when(productService.getAllProductsForCrawler()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/products/all-for-crawler")
                        .header("X-Crawler-Token", "test-crawler-token"))
                .andExpect(status().isOk());
    }
}
