package com.deliveranything.domain.auth.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.auth.dto.RefreshTokenDto;
import com.deliveranything.domain.auth.auth.repository.RefreshTokenRepository;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService 단위 테스트")
class RefreshTokenServiceTest {

  @Mock
  private AccessTokenService accessTokenService;

  @Mock
  private TokenBlacklistService tokenBlacklistService;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TokenRefreshRateLimiter rateLimiter;

  @InjectMocks
  private RefreshTokenService refreshTokenService;

  @Nested
  @DisplayName("Refresh Token 생성 테스트")
  class GenRefreshTokenTest {

    @Test
    @DisplayName("성공 - Refresh Token 생성")
    void genRefreshToken_success() {
      ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationDays", 7);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      String deviceInfo = "Chrome";

      doNothing().when(refreshTokenRepository).deleteByUserAndDevice(1L, deviceInfo);
      doNothing().when(refreshTokenRepository).save(any(RefreshTokenDto.class));

      String token = refreshTokenService.genRefreshToken(mockUser, deviceInfo);

      assertNotNull(token);
      verify(refreshTokenRepository, times(1)).deleteByUserAndDevice(1L, deviceInfo);
      verify(refreshTokenRepository, times(1)).save(any(RefreshTokenDto.class));
    }
  }

  @Nested
  @DisplayName("Refresh Token으로 사용자 조회 테스트")
  class GetUserByRefreshTokenTest {

    @Test
    @DisplayName("성공 - 유효한 Refresh Token")
    void getUserByRefreshToken_success() {
      String tokenValue = "valid-token";
      Long userId = 1L;

      RefreshTokenDto mockToken = mock(RefreshTokenDto.class);
      when(mockToken.getUserId()).thenReturn(userId);
      when(mockToken.isValid()).thenReturn(true);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(userId);

      when(refreshTokenRepository.findByTokenValue(tokenValue))
          .thenReturn(Optional.of(mockToken));
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

      User result = refreshTokenService.getUserByRefreshToken(tokenValue);

      assertNotNull(result);
      assertEquals(userId, result.getId());
      verify(refreshTokenRepository, times(1)).findByTokenValue(tokenValue);
      verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("실패 - 토큰을 찾을 수 없음")
    void getUserByRefreshToken_fail_not_found() {
      String tokenValue = "invalid-token";
      when(refreshTokenRepository.findByTokenValue(tokenValue))
          .thenReturn(Optional.empty());

      assertThrows(CustomException.class, () -> {
        refreshTokenService.getUserByRefreshToken(tokenValue);
      });
    }

    @Test
    @DisplayName("실패 - 만료된 토큰")
    void getUserByRefreshToken_fail_expired() {
      String tokenValue = "expired-token";

      RefreshTokenDto mockToken = mock(RefreshTokenDto.class);
      when(mockToken.isValid()).thenReturn(false);

      when(refreshTokenRepository.findByTokenValue(tokenValue))
          .thenReturn(Optional.of(mockToken));

      assertThrows(CustomException.class, () -> {
        refreshTokenService.getUserByRefreshToken(tokenValue);
      });
    }
  }

  @Nested
  @DisplayName("Refresh Token 무효화 테스트")
  class InvalidateRefreshTokenTest {

    @Test
    @DisplayName("성공 - 단일 기기 토큰 무효화")
    void invalidateRefreshToken_success() {
      Long userId = 1L;
      String deviceInfo = "Chrome";

      doNothing().when(refreshTokenRepository).deleteByUserAndDevice(userId, deviceInfo);

      refreshTokenService.invalidateRefreshToken(userId, deviceInfo);

      verify(refreshTokenRepository, times(1)).deleteByUserAndDevice(userId, deviceInfo);
    }

    @Test
    @DisplayName("성공 - 모든 기기 토큰 무효화")
    void invalidateAllRefreshTokens_success() {
      Long userId = 1L;

      doNothing().when(refreshTokenRepository).deleteAllByUser(userId);

      refreshTokenService.invalidateAllRefreshTokens(userId);

      verify(refreshTokenRepository, times(1)).deleteAllByUser(userId);
    }
  }

  @Nested
  @DisplayName("Access Token 재발급 테스트")
  class RefreshAccessTokenTest {

    @Test
    @DisplayName("성공 - Access Token 재발급")
    void refreshAccessToken_success() {
      String refreshTokenValue = "valid-refresh-token";
      String oldAccessToken = "old-access-token";

      RefreshTokenDto mockToken = mock(RefreshTokenDto.class);
      when(mockToken.getUserId()).thenReturn(1L);
      when(mockToken.isValid()).thenReturn(true);

      User mockUser = mock(User.class);

      when(mockUser.getId()).thenReturn(1L);

      when(refreshTokenRepository.findByTokenValue(refreshTokenValue))
          .thenReturn(Optional.of(mockToken));
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      // Rate Limiter Mock
      doNothing().when(rateLimiter).checkAndIncrementRefreshAttempt(1L);
      when(rateLimiter.getRemainingAttempts(1L)).thenReturn(4);

      // Access Token 관련 Mock
      when(accessTokenService.isValidToken(oldAccessToken)).thenReturn(true);
      when(accessTokenService.isTokenExpired(oldAccessToken)).thenReturn(false);
      doNothing().when(tokenBlacklistService).addToBlacklist(oldAccessToken);

      when(accessTokenService.genAccessToken(mockUser)).thenReturn("new-access-token");

      String newAccessToken = refreshTokenService.refreshAccessToken(refreshTokenValue,
          oldAccessToken);

      assertEquals("new-access-token", newAccessToken);
      verify(rateLimiter, times(1)).checkAndIncrementRefreshAttempt(1L);
      verify(tokenBlacklistService, times(1)).addToBlacklist(oldAccessToken);
      verify(accessTokenService, times(1)).genAccessToken(mockUser);
    }

    @Test
    @DisplayName("성공 - 기존 토큰 없이 재발급")
    void refreshAccessToken_without_old_token() {
      String refreshTokenValue = "valid-refresh-token";

      RefreshTokenDto mockToken = mock(RefreshTokenDto.class);
      when(mockToken.getUserId()).thenReturn(1L);
      when(mockToken.isValid()).thenReturn(true);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(refreshTokenRepository.findByTokenValue(refreshTokenValue))
          .thenReturn(Optional.of(mockToken));
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      doNothing().when(rateLimiter).checkAndIncrementRefreshAttempt(1L);
      when(rateLimiter.getRemainingAttempts(1L)).thenReturn(4);

      when(accessTokenService.genAccessToken(mockUser)).thenReturn("new-access-token");

      String newAccessToken = refreshTokenService.refreshAccessToken(refreshTokenValue, null);

      assertEquals("new-access-token", newAccessToken);
      verify(tokenBlacklistService, never()).addToBlacklist(anyString());
    }

    @Test
    @DisplayName("실패 - Rate Limit 초과")
    void refreshAccessToken_fail_rate_limit() {
      String refreshTokenValue = "valid-refresh-token";

      RefreshTokenDto mockToken = mock(RefreshTokenDto.class);
      when(mockToken.getUserId()).thenReturn(1L);
      when(mockToken.isValid()).thenReturn(true);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(refreshTokenRepository.findByTokenValue(refreshTokenValue))
          .thenReturn(Optional.of(mockToken));
      when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

      doThrow(new CustomException(
          com.deliveranything.global.exception.ErrorCode.TOKEN_REFRESH_RATE_LIMIT_EXCEEDED))
          .when(rateLimiter).checkAndIncrementRefreshAttempt(1L);

      assertThrows(CustomException.class, () -> {
        refreshTokenService.refreshAccessToken(refreshTokenValue, null);
      });

      verify(accessTokenService, never()).genAccessToken(any());
    }
  }
}