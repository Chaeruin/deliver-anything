package com.deliveranything.domain.auth.service;

import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 토큰 재발급 Rate Limiting 서비스 Redis를 사용하여 사용자별 토큰 재발급 횟수 제한
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshRateLimiter {

  private final RedisTemplate<String, String> redisTemplate;

  // 설정값
  private static final String RATE_LIMIT_PREFIX = "token_refresh_limit:";
  private static final int MAX_ATTEMPTS = 20;  // 최대 시도 횟수
  private static final int WINDOW_SECONDS = 60;  // 시간 윈도우 (초)

  /**
   * 토큰 재발급 시도 가능 여부 확인 및 카운트 증가
   *
   * @param userId 사용자 ID
   * @throws CustomException 제한 초과 시 TOKEN_REFRESH_RATE_LIMIT_EXCEEDED 예외 발생
   */
  public void checkAndIncrementRefreshAttempt(Long userId) {
    String key = RATE_LIMIT_PREFIX + userId;

    // 현재 시도 횟수 조회
    String countStr = redisTemplate.opsForValue().get(key);
    int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

    // 제한 초과 체크
    if (currentCount >= MAX_ATTEMPTS) {
      Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
      log.warn("토큰 재발급 제한 초과: userId={}, attempts={}, 남은시간={}초",
          userId, currentCount, ttl != null ? ttl : 0);
      throw new CustomException(ErrorCode.TOKEN_REFRESH_RATE_LIMIT_EXCEEDED);
    }

    // 카운트 증가
    if (currentCount == 0) {
      // 첫 시도: 키 생성 및 TTL 설정
      redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(WINDOW_SECONDS));
      log.debug("토큰 재발급 시도 카운트 시작: userId={}, count=1/{}", userId, MAX_ATTEMPTS);
    } else {
      // 기존 키: 카운트만 증가 (TTL 유지)
      redisTemplate.opsForValue().increment(key);
      log.debug("토큰 재발급 시도 카운트: userId={}, count={}/{}",
          userId, currentCount + 1, MAX_ATTEMPTS);
    }
  }

  /**
   * 남은 시도 횟수 조회
   *
   * @param userId 사용자 ID
   * @return 남은 시도 횟수
   */
  public int getRemainingAttempts(Long userId) {
    String key = RATE_LIMIT_PREFIX + userId;
    String countStr = redisTemplate.opsForValue().get(key);
    int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
    return Math.max(0, MAX_ATTEMPTS - currentCount);
  }

  /**
   * 남은 제한 시간 조회 (초)
   *
   * @param userId 사용자 ID
   * @return 제한이 해제되기까지 남은 시간 (초), 제한이 없으면 0
   */
  public long getRemainingTime(Long userId) {
    String key = RATE_LIMIT_PREFIX + userId;
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    return ttl != null && ttl > 0 ? ttl : 0;
  }

  /**
   * 카운트 초기화 (관리자용 또는 테스트용)
   *
   * @param userId 사용자 ID
   */
  public void resetRefreshAttempt(Long userId) {
    String key = RATE_LIMIT_PREFIX + userId;
    Boolean deleted = redisTemplate.delete(key);
    if (Boolean.TRUE.equals(deleted)) {
      log.info("토큰 재발급 카운트 초기화: userId={}", userId);
    }
  }
}