package com.deliveranything.domain.auth.repository;

import com.deliveranything.domain.auth.dto.RedisRefreshTokenDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${custom.refreshToken.expirationDays}")
  private int refreshTokenExpirationDays;

  private static final String KEY_PREFIX = "refresh_token:";
  private static final String TOKEN_INDEX_PREFIX = "token_index:";

  /**
   * Refresh Token 저장 (14일 TTL)
   */
  public void save(RedisRefreshTokenDto token) {
    String key = RedisRefreshTokenDto.generateKey(token.getUserId(), token.getDeviceInfo());

    // 1. 토큰 데이터 저장
    redisTemplate.opsForValue().set(key, token, refreshTokenExpirationDays, TimeUnit.DAYS);

    // 2. 토큰 값으로 키를 찾을 수 있도록 인덱스 저장 (역참조용)
    String indexKey = TOKEN_INDEX_PREFIX + token.getTokenValue();
    redisTemplate.opsForValue().set(indexKey, key, refreshTokenExpirationDays, TimeUnit.DAYS);

    log.info("Redis에 RefreshToken 저장: userId={}, deviceInfo={}, ttl={}일",
        token.getUserId(), token.getDeviceInfo(), refreshTokenExpirationDays);
  }

  /**
   * 토큰 값으로 조회
   */
  public Optional<RedisRefreshTokenDto> findByTokenValue(String tokenValue) {
    // 1. 인덱스로 키 찾기
    String indexKey = TOKEN_INDEX_PREFIX + tokenValue;
    String key = (String) redisTemplate.opsForValue().get(indexKey);

    if (key == null) {
      return Optional.empty();
    }

    // 2. 실제 토큰 데이터 조회
    Object data = redisTemplate.opsForValue().get(key);
    if (data == null) {
      return Optional.empty();
    }
    RedisRefreshTokenDto token = objectMapper.convertValue(data, RedisRefreshTokenDto.class);
    return Optional.ofNullable(token);
  }

  /**
   * 사용자 + 디바이스로 조회
   */
  public Optional<RedisRefreshTokenDto> findByUserAndDevice(Long userId, String deviceInfo) {
    String key = RedisRefreshTokenDto.generateKey(userId, deviceInfo);
    Object data = redisTemplate.opsForValue().get(key);
    if (data == null) {
      return Optional.empty();
    }
    RedisRefreshTokenDto token = objectMapper.convertValue(data, RedisRefreshTokenDto.class);
    return Optional.ofNullable(token);
  }

  /**
   * 특정 디바이스 토큰 삭제
   */
  public void deleteByUserAndDevice(Long userId, String deviceInfo) {
    findByUserAndDevice(userId, deviceInfo).ifPresent(token -> {
      String key = RedisRefreshTokenDto.generateKey(userId, deviceInfo);
      String indexKey = TOKEN_INDEX_PREFIX + token.getTokenValue();

      redisTemplate.delete(key);
      redisTemplate.delete(indexKey);

      log.info("Redis에서 RefreshToken 삭제: userId={}, deviceInfo={}", userId, deviceInfo);
    });
  }

  /**
   * 사용자의 모든 토큰 삭제 (전체 로그아웃)
   */
  public void deleteAllByUser(Long userId) {
    String pattern = KEY_PREFIX + userId + ":*";
    Set<String> keys = redisTemplate.keys(pattern);

    if (keys != null && !keys.isEmpty()) {
      // 각 토큰의 인덱스도 삭제
      keys.forEach(key -> {
        Object data = redisTemplate.opsForValue().get(key);
        if (data != null) {
          RedisRefreshTokenDto token = objectMapper.convertValue(data, RedisRefreshTokenDto.class);
          String indexKey = TOKEN_INDEX_PREFIX + token.getTokenValue();
          redisTemplate.delete(indexKey);
        }
      });

      // 토큰 데이터 삭제
      redisTemplate.delete(keys);
      log.info("Redis에서 사용자의 모든 RefreshToken 삭제: userId={}, count={}",
          userId, keys.size());
    }
  }

  /**
   * 토큰 존재 여부 확인
   */
  public boolean existsByTokenValue(String tokenValue) {
    String indexKey = TOKEN_INDEX_PREFIX + tokenValue;
    return Boolean.TRUE.equals(redisTemplate.hasKey(indexKey));
  }
}
