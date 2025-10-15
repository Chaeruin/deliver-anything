package com.deliveranything.domain.auth.service;

import com.deliveranything.domain.auth.dto.RefreshTokenDto;
import com.deliveranything.domain.auth.repository.RefreshTokenRepository;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

  private final AccessTokenService accessTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final TokenRefreshRateLimiter rateLimiter;

  @Value("${custom.refreshToken.expirationDays}")
  private int refreshTokenExpirationDays;


  /**
   * RefreshToken 생성 (Redis 저장)
   */
  @Transactional
  public String genRefreshToken(User user, String deviceInfo) {
    // 1. 기존 디바이스 토큰 삭제 (Redis)
    refreshTokenRepository.deleteByUserAndDevice(user.getId(), deviceInfo);

    // 2. 새 토큰 생성
    String tokenValue = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusDays(refreshTokenExpirationDays);

    RefreshTokenDto redisToken = RefreshTokenDto.builder()
        .userId(user.getId())
        .tokenValue(tokenValue)
        .deviceInfo(deviceInfo)
        .expiresAt(expiresAt)
        .createdAt(LocalDateTime.now())
        .build();

    // 3. Redis에 저장
    refreshTokenRepository.save(redisToken);

    log.info("RefreshToken 생성 (Redis): userId={}, deviceInfo={}",
        user.getId(), deviceInfo);

    return tokenValue;
  }

  /**
   * RefreshToken으로 사용자 조회 (Redis)
   */
  public String refreshAccessToken(String refreshTokenValue, String oldAccessToken) {
    // 1. 사용자 조회 및 검증 (Rate Limiting 전에 토큰 유효성 먼저 확인)
    User user = getUserByRefreshToken(refreshTokenValue);

    // 2. Rate Limiting 체크 (1분에 5회 제한)
    try {
      rateLimiter.checkAndIncrementRefreshAttempt(user.getId());
    } catch (CustomException e) {
      // Rate Limit 초과 시 상세 로그
      long remainingTime = rateLimiter.getRemainingTime(user.getId());
      log.warn("토큰 재발급 Rate Limit 초과: userId={}, 다음 시도까지 {}초 대기 필요",
          user.getId(), remainingTime);
      throw e;
    }

    // 3. 기존 Access Token 블랙리스트 처리
    if (oldAccessToken != null && !oldAccessToken.isBlank()) {
      if (accessTokenService.isValidToken(oldAccessToken)
          && !accessTokenService.isTokenExpired(oldAccessToken)) {
        tokenBlacklistService.addToBlacklist(oldAccessToken);
        log.info("토큰 재발급: 기존 Access Token 블랙리스트 등록 완료 - userId={}", user.getId());
      } else {
        log.debug("기존 Access Token이 이미 만료되었거나 유효하지 않음 - 블랙리스트 스킵: userId={}",
            user.getId());
      }
    } else {
      log.debug("기존 Access Token이 제공되지 않음 (정상: 토큰 만료 후 재발급): userId={}",
          user.getId());
    }

    // 4. 새 Access Token 발급
    String newAccessToken = accessTokenService.genAccessToken(user);

    int remainingAttempts = rateLimiter.getRemainingAttempts(user.getId());
    log.info("새 Access Token 발급 완료: userId={}, 남은 재발급 횟수={}/5",
        user.getId(), remainingAttempts);

    return newAccessToken;
  }

  /**
   * RefreshToken으로 사용자 조회 (Redis)
   */
  public User getUserByRefreshToken(String refreshTokenValue) {
    // null 체크
    if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
      log.warn("Refresh Token 값이 null 또는 빈 문자열입니다.");
      throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    // 1. Redis에서 토큰 조회 (인덱스 사용)
    RefreshTokenDto redisToken = refreshTokenRepository
        .findByTokenValue(refreshTokenValue)
        .orElseThrow(() -> {
          log.warn("Redis에서 Refresh Token을 찾을 수 없습니다: {}",
              refreshTokenValue.substring(0, Math.min(8, refreshTokenValue.length())));
          return new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        });

    // 2. 유효성 검증
    if (!redisToken.isValid()) {
      log.warn("만료된 Refresh Token: userId={}", redisToken.getUserId());
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    // 3. 사용자 조회
    return userRepository.findById(redisToken.getUserId())
        .orElseThrow(() -> {
          log.error("Refresh Token은 존재하지만 사용자를 찾을 수 없음: userId={}",
              redisToken.getUserId());
          return new CustomException(ErrorCode.USER_NOT_FOUND);
        });
  }

  /**
   * 특정 디바이스의 RefreshToken 무효화 (로그아웃)
   */
  @Transactional
  public void invalidateRefreshToken(Long userId, String deviceInfo) {
    refreshTokenRepository.deleteByUserAndDevice(userId, deviceInfo);
    log.info("RefreshToken 무효화 (Redis): userId={}, deviceInfo={}", userId, deviceInfo);
  }

  /**
   * 모든 기기의 RefreshToken 무효화 (전체 로그아웃)
   */
  @Transactional
  public void invalidateAllRefreshTokens(Long userId) {
    refreshTokenRepository.deleteAllByUser(userId);
    log.info("모든 RefreshToken 무효화 (Redis): userId={}", userId);
  }

}