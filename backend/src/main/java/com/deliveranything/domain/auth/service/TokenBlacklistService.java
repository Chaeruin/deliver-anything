package com.deliveranything.domain.auth.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private final RedisTemplate<String, String> redisTemplate;
  private final AuthTokenService authTokenService;

  private static final String BLACKLIST_PREFIX = "token:blacklist:";

  /**
   * accessToken을 블랙리스트에 추가
   */
  public void addToBlacklist(String accessToken) {
    try {
      // 토큰 만료까지 남은 시간 계산
      long expirationTime = authTokenService.getExpirationTime(accessToken);
      long now = System.currentTimeMillis();
      long ttl = expirationTime - now;

      // 이미 만료된 토큰은 블랙리스트에 추가할 필요 없음
      if (ttl <= 0) {
        log.debug("이미 만료된 토큰, 블랙리스트 추가 건너뜀");
        return;
      }

      String key = BLACKLIST_PREFIX + accessToken;
      redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));

      log.info("accessToken 블랙리스트 등록: ttl={}초", ttl / 1000);
    } catch (Exception e) {
      log.error("블랙리스트 등록 실패: {}", e.getMessage(), e);
    }
  }

  /**
   * accessToken이 블랙리스트에 있는지 확인
   */
  public boolean isBlacklisted(String accessToken) {
    String key = BLACKLIST_PREFIX + accessToken;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}