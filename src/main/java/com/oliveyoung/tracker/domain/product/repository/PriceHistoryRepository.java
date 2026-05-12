package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByRecordedAtAsc(Long productId);

    List<PriceHistory> findByProductIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long productId, LocalDateTime from, LocalDateTime to);

    Optional<PriceHistory> findTopByProductIdOrderByRecordedAtDesc(Long productId);

    @Query("SELECT MIN(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = :productId")
    Optional<Integer> findLowestPriceByProductId(@Param("productId") Long productId);

    @Query("SELECT MAX(ph.currentPrice) FROM PriceHistory ph WHERE ph.product.id = :productId")
    Optional<Integer> findHighestPriceByProductId(@Param("productId") Long productId);

    void deleteByRecordedAtBefore(LocalDateTime cutoff);
}
