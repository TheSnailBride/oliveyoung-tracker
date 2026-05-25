package com.oliveyoung.tracker.crawler;

import com.oliveyoung.tracker.common.response.ApiResponse;
import com.oliveyoung.tracker.crawler.dto.CrawledProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerScheduler crawlerScheduler;

    /**
     * 수동 전체 크롤링 (전 카테고리 랭킹 + DB 기존 상품 업데이트)
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<String>> runCrawler() {
        log.info("수동 크롤링 요청");
        boolean started = crawlerScheduler.startManualCrawling();
        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 크롤링이 실행 중입니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok("전체 카테고리 랭킹 크롤링이 시작되었습니다."));
    }

    /**
     * 외부 크롤러(Python 등)에서 수집한 데이터를 JSON으로 받아 DB에 저장
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<String>> importCrawledData(@RequestBody List<CrawledProduct> products) {
        log.info("외부 크롤링 데이터 임포트: {}개", products.size());
        int[] result = crawlerScheduler.saveCrawledProducts(products);
        String msg = String.format("신규: %d개, 업데이트: %d개", result[0], result[1]);
        log.info("임포트 완료 - {}", msg);
        return ResponseEntity.ok(ApiResponse.ok(msg));
    }
}
