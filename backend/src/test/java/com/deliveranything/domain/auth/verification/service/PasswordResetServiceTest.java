package com.deliveranything.domain.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.auth.enums.SocialProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService 단위 테스트")
class PasswordResetServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private VerificationService verificationService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private PasswordResetService passwordResetService;

  @Nested
  @DisplayName("비밀번호 재설정 요청 테스트")
  class RequestPasswordResetTest {

    @Test
    @DisplayName("성공 - 일반 로그인 사용자")
    void requestPasswordReset_success() {
      // Given
      String email = "test@test.com";
      User user = User.builder()
          .email(email)
          .username("테스트")
          .password("encoded-password")
          .socialProvider(SocialProvider.LOCAL)
          .build();

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      doNothing().when(verificationService).sendVerificationCode(any());

      // When & Then
      assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(email));
      verify(verificationService, times(1)).sendVerificationCode(any());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 이메일")
    void requestPasswordReset_fail_user_not_found() {
      // Given
      String email = "notfound@test.com";
      when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(email);
      });

      assertEquals("USER-404", exception.getCode());
      verify(verificationService, never()).sendVerificationCode(any());
    }

    @Test
    @DisplayName("실패 - 소셜 로그인 사용자 (Google)")
    void requestPasswordReset_fail_google_user() {
      // Given
      String email = "google@test.com";
      User user = User.builder()
          .email(email)
          .username("구글유저")
          .socialProvider(SocialProvider.GOOGLE)
          .socialId("google-123")
          .build();

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(email);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(verificationService, never()).sendVerificationCode(any());
    }

    @Test
    @DisplayName("실패 - 소셜 로그인 사용자 (Kakao)")
    void requestPasswordReset_fail_kakao_user() {
      // Given
      String email = "kakao@test.com";
      User user = User.builder()
          .email(email)
          .username("카카오유저")
          .socialProvider(SocialProvider.KAKAO)
          .socialId("kakao-456")
          .build();

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(email);
      });

      assertEquals("AUTH-401", exception.getCode());
    }

    @Test
    @DisplayName("실패 - 소셜 로그인 사용자 (Naver)")
    void requestPasswordReset_fail_naver_user() {
      // Given
      String email = "naver@test.com";
      User user = User.builder()
          .email(email)
          .username("네이버유저")
          .socialProvider(SocialProvider.NAVER)
          .socialId("naver-789")
          .build();

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(email);
      });

      assertEquals("AUTH-401", exception.getCode());
    }
  }

  @Nested
  @DisplayName("인증 코드 검증 및 토큰 발급 테스트")
  class VerifyCodeAndIssueResetTokenTest {

    @Test
    @DisplayName("성공 - 올바른 인증 코드로 토큰 발급")
    void verifyCodeAndIssueResetToken_success() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      User user = User.builder()
          .email(email)
          .username("테스트")
          .password("encoded-password")
          .build();
      ReflectionTestUtils.setField(user, "id", 1L);

      when(verificationService.verifyCode(any())).thenReturn(true);
      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

      // When
      String resetToken = passwordResetService.verifyCodeAndIssueResetToken(email, code);

      // Then
      assertNotNull(resetToken);
      verify(verificationService, times(1)).verifyCode(any());
      verify(valueOperations, times(1)).set(anyString(), eq("1"), any(Duration.class));
    }

    @Test
    @DisplayName("실패 - 잘못된 인증 코드")
    void verifyCodeAndIssueResetToken_fail_invalid_code() {
      // Given
      String email = "test@test.com";
      String wrongCode = "999999";

      when(verificationService.verifyCode(any()))
          .thenThrow(new CustomException(
              com.deliveranything.global.exception.ErrorCode.TOKEN_INVALID));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.verifyCodeAndIssueResetToken(email, wrongCode);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("실패 - 인증 성공했지만 사용자를 찾을 수 없음")
    void verifyCodeAndIssueResetToken_fail_user_not_found() {
      // Given
      String email = "notfound@test.com";
      String code = "123456";

      when(verificationService.verifyCode(any())).thenReturn(true);
      when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.verifyCodeAndIssueResetToken(email, code);
      });

      assertEquals("USER-404", exception.getCode());
      verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }
  }

  @Nested
  @DisplayName("비밀번호 재설정 확정 테스트")
  class ResetPasswordTest {

    @Test
    @DisplayName("성공 - 유효한 토큰으로 비밀번호 변경")
    void resetPassword_success() {
      // Given
      String resetToken = "valid-reset-token";
      String newPassword = "NewPassword123!";
      Long userId = 1L;

      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("old-encoded-password")
          .build();
      ReflectionTestUtils.setField(user, "id", userId);

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(userId.toString());
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");
      when(userRepository.save(user)).thenReturn(user);
      when(redisTemplate.delete(anyString())).thenReturn(true);

      // When & Then
      assertDoesNotThrow(() -> passwordResetService.resetPassword(resetToken, newPassword));

      verify(passwordEncoder, times(1)).encode(newPassword);
      verify(userRepository, times(1)).save(user);
      verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("실패 - 만료된 토큰")
    void resetPassword_fail_expired_token() {
      // Given
      String expiredToken = "expired-token";
      String newPassword = "NewPassword123!";

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(null); // Redis에 없음

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(expiredToken, newPassword);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(userRepository, never()).findById(any());
      verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 토큰")
    void resetPassword_fail_invalid_token() {
      // Given
      String invalidToken = "invalid-token";
      String newPassword = "NewPassword123!";

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(null);

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(invalidToken, newPassword);
      });

      assertEquals("AUTH-401", exception.getCode());
    }

    @Test
    @DisplayName("실패 - 토큰은 유효하지만 사용자를 찾을 수 없음")
    void resetPassword_fail_user_not_found() {
      // Given
      String resetToken = "valid-token";
      String newPassword = "NewPassword123!";
      Long userId = 999L;

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(userId.toString());
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(resetToken, newPassword);
      });

      assertEquals("USER-404", exception.getCode());
      verify(passwordEncoder, never()).encode(anyString());
      verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("성공 - 토큰은 일회용 (재사용 불가)")
    void resetPassword_token_is_single_use() {
      // Given
      String resetToken = "valid-token";
      String newPassword = "NewPassword123!";
      Long userId = 1L;

      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("old-password")
          .build();
      ReflectionTestUtils.setField(user, "id", userId);

      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(userId.toString());
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");
      when(userRepository.save(user)).thenReturn(user);
      when(redisTemplate.delete(anyString())).thenReturn(true);

      // When
      passwordResetService.resetPassword(resetToken, newPassword);

      // Then - 토큰 삭제 확인
      verify(redisTemplate, times(1)).delete(anyString());

      // 같은 토큰으로 다시 시도
      when(valueOperations.get(anyString())).thenReturn(null); // 이미 삭제됨

      // Then - 재사용 불가
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(resetToken, "AnotherPassword123!");
      });

      assertEquals("AUTH-401", exception.getCode());
    }
  }

  @Nested
  @DisplayName("통합 시나리오 테스트")
  class IntegrationScenarioTest {

    @Test
    @DisplayName("성공 - 전체 플로우: 요청 → 검증 → 재설정")
    void full_password_reset_flow_success() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      String newPassword = "NewPassword123!";
      Long userId = 1L;

      User user = User.builder()
          .email(email)
          .username("테스트")
          .password("old-password")
          .socialProvider(SocialProvider.LOCAL)
          .build();
      ReflectionTestUtils.setField(user, "id", userId);

      // 1단계: 비밀번호 재설정 요청
      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      doNothing().when(verificationService).sendVerificationCode(any());

      assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(email));

      // 2단계: 인증 코드 검증 및 토큰 발급
      when(verificationService.verifyCode(any())).thenReturn(true);
      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

      String resetToken = passwordResetService.verifyCodeAndIssueResetToken(email, code);
      assertNotNull(resetToken);

      // 3단계: 새 비밀번호 설정
      when(valueOperations.get(anyString())).thenReturn(userId.toString());
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");
      when(userRepository.save(user)).thenReturn(user);
      when(redisTemplate.delete(anyString())).thenReturn(true);

      assertDoesNotThrow(() -> passwordResetService.resetPassword(resetToken, newPassword));

      // Then
      verify(verificationService, times(1)).sendVerificationCode(any());
      verify(verificationService, times(1)).verifyCode(any());
      verify(passwordEncoder, times(1)).encode(newPassword);
      verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("실패 - 소셜 로그인 사용자는 첫 단계에서 차단")
    void full_flow_fail_social_user_blocked_at_first_step() {
      // Given
      String email = "google@test.com";
      User googleUser = User.builder()
          .email(email)
          .username("구글유저")
          .socialProvider(SocialProvider.GOOGLE)
          .socialId("google-123")
          .build();

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(googleUser));

      // When & Then - 첫 단계에서 실패
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(email);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(verificationService, never()).sendVerificationCode(any());
    }

    @Test
    @DisplayName("실패 - 잘못된 인증 코드로 두 번째 단계 실패")
    void full_flow_fail_at_second_step() {
      // Given
      String email = "test@test.com";
      String wrongCode = "999999";

      User user = User.builder()
          .email(email)
          .username("테스트")
          .socialProvider(SocialProvider.LOCAL)
          .build();

      // 1단계 성공
      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      doNothing().when(verificationService).sendVerificationCode(any());
      assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(email));

      // 2단계 실패
      when(verificationService.verifyCode(any()))
          .thenThrow(new CustomException(
              com.deliveranything.global.exception.ErrorCode.TOKEN_INVALID));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.verifyCodeAndIssueResetToken(email, wrongCode);
      });

      assertEquals("AUTH-401", exception.getCode());
    }

    @Test
    @DisplayName("실패 - 10분 후 토큰 만료로 세 번째 단계 실패")
    void full_flow_fail_token_expired_at_third_step() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      String newPassword = "NewPassword123!";
      Long userId = 1L;

      User user = User.builder()
          .email(email)
          .username("테스트")
          .socialProvider(SocialProvider.LOCAL)
          .build();
      ReflectionTestUtils.setField(user, "id", userId);

      // 1단계 성공
      when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
      doNothing().when(verificationService).sendVerificationCode(any());
      assertDoesNotThrow(() -> passwordResetService.requestPasswordReset(email));

      // 2단계 성공
      when(verificationService.verifyCode(any())).thenReturn(true);
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));
      String resetToken = passwordResetService.verifyCodeAndIssueResetToken(email, code);

      // 3단계 실패 (토큰 만료 - 10분 경과)
      when(valueOperations.get(anyString())).thenReturn(null); // 10분 후 만료

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(resetToken, newPassword);
      });

      assertEquals("AUTH-401", exception.getCode());
    }
  }

  @Nested
  @DisplayName("보안 테스트")
  class SecurityTest {

    @Test
    @DisplayName("보안 - 존재하지 않는 이메일로 정보 노출 방지")
    void security_no_information_disclosure() {
      // Given
      String nonExistentEmail = "notfound@test.com";
      when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.requestPasswordReset(nonExistentEmail);
      });

      // 사용자 존재 여부를 알 수 없도록 같은 에러 반환
      assertEquals("USER-404", exception.getCode());
    }

    @Test
    @DisplayName("보안 - 토큰 재사용 불가")
    void security_token_cannot_be_reused() {
      // Given
      String resetToken = "used-token";
      String newPassword = "NewPassword123!";
      Long userId = 1L;

      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("old-password")
          .build();
      ReflectionTestUtils.setField(user, "id", userId);

      // 첫 번째 사용
      when(redisTemplate.opsForValue()).thenReturn(valueOperations);
      when(valueOperations.get(anyString())).thenReturn(userId.toString());
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");
      when(userRepository.save(user)).thenReturn(user);
      when(redisTemplate.delete(anyString())).thenReturn(true);

      passwordResetService.resetPassword(resetToken, newPassword);

      // 두 번째 사용 시도
      when(valueOperations.get(anyString())).thenReturn(null); // 이미 삭제됨

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        passwordResetService.resetPassword(resetToken, "AnotherPassword456!");
      });

      assertEquals("AUTH-401", exception.getCode());
    }

    @Test
    @DisplayName("보안 - 소셜 로그인 사용자 차단 (모든 제공자)")
    void security_block_all_social_providers() {
      // Given
      SocialProvider[] socialProviders = {
          SocialProvider.GOOGLE,
          SocialProvider.KAKAO,
          SocialProvider.NAVER
      };

      for (SocialProvider provider : socialProviders) {
        String email = provider.name().toLowerCase() + "@test.com";
        User socialUser = User.builder()
            .email(email)
            .username(provider.name() + " User")
            .socialProvider(provider)
            .socialId(provider.name() + "-123")
            .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(socialUser));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
          passwordResetService.requestPasswordReset(email);
        });

        assertEquals("AUTH-401", exception.getCode());
      }
    }
  }
}