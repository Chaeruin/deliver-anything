package com.deliveranything.domain.auth.auth.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService 단위 테스트")
class TokenBlacklistServiceTest {

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private AccessTokenService accessTokenService;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private TokenBlacklistService tokenBlacklistService;

  @Nested
  @DisplayName("블랙리스트 추가 테스트")
  class AddToBlacklistTest {

    @Test
    @DisplayName("성공 - 유효한 토큰 블랙리스트 추가")
    void addToBlacklist_success() {
      String accessToken = "valid-token";
      long now = System.currentTimeMillis();
      long expirationTime = now + 3600000;

      when(accessTokenService.getExpirationTime(accessToken)).thenReturn(expirationTime);
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

      tokenBlacklistService.addToBlacklist(accessToken);

      verify(redisTemplate, times(1)).opsForValue();
      verify(valueOperations, times(1)).set(
          eq("token:blacklist:" + accessToken),
          eq("blacklisted"),
          any(Duration.class)
      );
    }

    @Test
    @DisplayName("건너뜀 - 이미 만료된 토큰")
    void addToBlacklist_skip_expired_token() {
      String accessToken = "expired-token";
      long now = System.currentTimeMillis();
      long expirationTime = now - 1000;

      when(accessTokenService.getExpirationTime(accessToken)).thenReturn(expirationTime);

      tokenBlacklistService.addToBlacklist(accessToken);

      verify(redisTemplate, never()).opsForValue();
    }
  }

  @Nested
  @DisplayName("블랙리스트 확인 테스트")
  class IsBlacklistedTest {

    @Test
    @DisplayName("True - 블랙리스트에 있는 토큰")
    void isBlacklisted_true() {
      String accessToken = "blacklisted-token";
      String key = "token:blacklist:" + accessToken;

      when(redisTemplate.hasKey(key)).thenReturn(true);

      boolean result = tokenBlacklistService.isBlacklisted(accessToken);

      assertTrue(result);
      verify(redisTemplate, times(1)).hasKey(key);
    }

    @Test
    @DisplayName("False - 블랙리스트에 없는 토큰")
    void isBlacklisted_false() {
      String accessToken = "valid-token";
      String key = "token:blacklist:" + accessToken;

      when(redisTemplate.hasKey(key)).thenReturn(false);

      boolean result = tokenBlacklistService.isBlacklisted(accessToken);

      assertFalse(result);
      verify(redisTemplate, times(1)).hasKey(key);
    }
  }
}