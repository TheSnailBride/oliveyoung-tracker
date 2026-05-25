package com.oliveyoung.tracker.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCrawlerRunLockTest {

    private static final String LOCK_KEY = "lock:crawler:manual-run";
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    @DisplayName("Redis SET NX EX로 크롤러 락을 획득하고 해제 시 소유 토큰을 검증한다")
    void tryAcquireReturnsLeaseAndReleasesOwnedLock() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), eq(TTL))).thenReturn(true);

        RedisCrawlerRunLock lock = new RedisCrawlerRunLock(redisTemplate, LOCK_KEY, TTL);

        Optional<CrawlerRunLock.Lease> lease = lock.tryAcquire();
        assertThat(lease).isPresent();

        lease.get().close();

        verify(valueOperations).setIfAbsent(eq(LOCK_KEY), anyString(), eq(TTL));
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(LOCK_KEY)), anyString());
    }

    @Test
    @DisplayName("Redis에 이미 크롤러 락이 있으면 실행 권한을 획득하지 못한다")
    void tryAcquireReturnsEmptyWhenLockAlreadyExists() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(LOCK_KEY), anyString(), eq(TTL))).thenReturn(false);

        RedisCrawlerRunLock lock = new RedisCrawlerRunLock(redisTemplate, LOCK_KEY, TTL);

        assertThat(lock.tryAcquire()).isEmpty();
    }
}
