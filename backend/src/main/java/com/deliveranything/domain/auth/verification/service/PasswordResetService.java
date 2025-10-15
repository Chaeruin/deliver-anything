package com.deliveranything.domain.auth.verification.service;

import com.deliveranything.domain.auth.verification.dto.VerificationSendRequest;
import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

  private final UserRepository userRepository;
  private final VerificationService verificationService;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, String> redisTemplate;

  @Value("${custom.email.verification.expirationMinutes}")
  private int verificationExpirationMinutes;

  private static final String RESET_TOKEN_PREFIX = "password_reset_token:";

  /**
   * 1단계: 비밀번호 재설정 요청 - 이메일로 인증 코드 발송
   */
  @Transactional
  public void requestPasswordReset(String email) {
    // 사용자 존재 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("존재하지 않는 이메일로 비밀번호 재설정 요청: {}", email);
          // 보안상 사용자가 없어도 성공 메시지 반환 (이메일 존재 여부 노출 방지)
          return new CustomException(ErrorCode.USER_NOT_FOUND);
        });

    // 소셜 로그인 사용자는 비밀번호 재설정 불가
    if (user.getSocialProvider()
        != com.deliveranything.domain.auth.auth.enums.SocialProvider.LOCAL) {
      log.warn("소셜 로그인 사용자 비밀번호 재설정 시도: email={}, provider={}",
          email, user.getSocialProvider());
      throw new CustomException(ErrorCode.TOKEN_INVALID);
    }

    // 인증 코드 발송 (VerificationService 재사용)
    verificationService.sendVerificationCode(
        new VerificationSendRequest(
            email,
            VerificationType.EMAIL,
            VerificationPurpose.PASSWORD_RESET
        )
    );

    log.info("비밀번호 재설정 인증 코드 발송 완료: {}, 만료시간: {}분", email, verificationExpirationMinutes);
  }

  /**
   * 2단계: 인증 코드 검증 후 재설정 토큰 발급
   */
  @Transactional
  public String verifyCodeAndIssueResetToken(String email, String verificationCode) {
    // 인증 코드 검증
    verificationService.verifyCode(
        new com.deliveranything.domain.auth.verification.dto.VerificationVerifyRequest(
            email,
            verificationCode,
            VerificationType.EMAIL,
            VerificationPurpose.PASSWORD_RESET
        )
    );

    // 사용자 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // 재설정 토큰 생성 (UUID)
    String resetToken = UUID.randomUUID().toString();

    // Redis에 토큰 저장 (인증 코드와 동일한 만료 시간 사용)
    String redisKey = RESET_TOKEN_PREFIX + resetToken;
    redisTemplate.opsForValue().set(
        redisKey,
        user.getId().toString(),
        Duration.ofMinutes(verificationExpirationMinutes)
    );

    log.info("비밀번호 재설정 토큰 발급: userId={}, 만료시간: {}분",
        user.getId(), verificationExpirationMinutes);
    return resetToken;
  }

  /**
   * 3단계: 재설정 토큰으로 비밀번호 변경
   */
  @Transactional
  public void resetPassword(String resetToken, String newPassword) {
    // Redis에서 토큰 확인
    String redisKey = RESET_TOKEN_PREFIX + resetToken;
    String userIdStr = redisTemplate.opsForValue().get(redisKey);

    if (userIdStr == null) {
      log.warn("만료되었거나 존재하지 않는 재설정 토큰: {}", resetToken);
      throw new CustomException(ErrorCode.TOKEN_EXPIRED);
    }

    Long userId = Long.parseLong(userIdStr);

    // 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // 비밀번호 변경
    String encodedPassword = passwordEncoder.encode(newPassword);
    user.updatePassword(encodedPassword);
    userRepository.save(user);

    // 토큰 삭제 (재사용 방지)
    redisTemplate.delete(redisKey);

    log.info("비밀번호 재설정 완료: userId={}", userId);
  }
}