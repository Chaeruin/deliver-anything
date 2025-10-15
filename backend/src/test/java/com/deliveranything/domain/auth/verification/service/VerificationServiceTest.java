package com.deliveranything.domain.auth.verification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.verification.dto.VerificationSendRequest;
import com.deliveranything.domain.auth.verification.dto.VerificationVerifyRequest;
import com.deliveranything.domain.auth.verification.entity.VerificationToken;
import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import com.deliveranything.domain.auth.verification.repository.VerificationTokenRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.infra.EmailService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationService 단위 테스트")
class VerificationServiceTest {

  @Mock
  private VerificationTokenRepository verificationTokenRepository;

  @Mock
  private EmailService emailService;

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @InjectMocks
  private VerificationService verificationService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(verificationService, "verificationExpirationMinutes", 5);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Nested
  @DisplayName("인증 코드 발송 테스트")
  class SendVerificationCodeTest {

    @Test
    @DisplayName("성공 - 이메일 인증 코드 발송")
    void sendVerificationCode_success() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn(null); // Rate limit 통과
      when(valueOperations.increment(anyString())).thenReturn(1L);
      when(verificationTokenRepository.save(any(VerificationToken.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

      // When & Then
      assertDoesNotThrow(() -> verificationService.sendVerificationCode(request));

      verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
      verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString());
      verify(valueOperations, times(1)).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("실패 - SMS 인증은 지원하지 않음")
    void sendVerificationCode_fail_sms_not_supported() {
      // Given
      VerificationSendRequest request = new VerificationSendRequest(
          "01012345678",
          VerificationType.PHONE,
          VerificationPurpose.SIGNUP
      );

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.sendVerificationCode(request);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("실패 - Rate Limit 초과 (1시간 5회)")
    void sendVerificationCode_fail_rate_limit_exceeded() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn("5"); // 이미 5회 발송

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.sendVerificationCode(request);
      });

      assertEquals("AUTH-429", exception.getCode());
      assertTrue(exception.getMessage().contains("시도 횟수를 초과"));
      verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
      verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("성공 - 기존 인증 코드 삭제 후 새로 발송")
    void sendVerificationCode_success_delete_existing() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn(null);
      when(valueOperations.increment(anyString())).thenReturn(1L);
      when(redisTemplate.delete(anyString())).thenReturn(true);
      when(verificationTokenRepository.save(any(VerificationToken.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

      // When
      assertDoesNotThrow(() -> verificationService.sendVerificationCode(request));

      // Then
      verify(redisTemplate, times(1)).delete(anyString());
      verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
    }
  }

  @Nested
  @DisplayName("인증 코드 검증 테스트")
  class VerifyCodeTest {

    @Test
    @DisplayName("성공 - 올바른 인증 코드")
    void verifyCode_success() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          email,
          code,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      VerificationToken token = VerificationToken.builder()
          .identifier(email)
          .verificationType(VerificationType.EMAIL)
          .purpose(VerificationPurpose.SIGNUP)
          .validMinutes(5)
          .build();
      ReflectionTestUtils.setField(token, "verificationCode", code);
      ReflectionTestUtils.setField(token, "isUsed", false);
      ReflectionTestUtils.setField(token, "expireAt", LocalDateTime.now().plusMinutes(5));

      when(valueOperations.get(anyString())).thenReturn(code); // Redis에서 코드 조회
      when(
          verificationTokenRepository.findTopByIdentifierAndVerificationTypeAndPurposeOrderByCreatedAtDesc(
              email, VerificationType.EMAIL, VerificationPurpose.SIGNUP))
          .thenReturn(Optional.of(token));
      when(verificationTokenRepository.save(token)).thenReturn(token);
      when(redisTemplate.delete(anyString())).thenReturn(true);

      // When
      boolean result = verificationService.verifyCode(request);

      // Then
      assertTrue(result);
      verify(redisTemplate, times(1)).delete(anyString());
      verify(verificationTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("실패 - 인증 코드가 만료됨")
    void verifyCode_fail_expired() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          email,
          code,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn(null); // Redis에 없음 (만료)

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.verifyCode(request);
      });

      assertEquals("AUTH-401", exception.getCode());
      assertTrue(exception.getMessage().contains("만료"));
      verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("실패 - 잘못된 인증 코드")
    void verifyCode_fail_invalid_code() {
      // Given
      String email = "test@test.com";
      String wrongCode = "999999";
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          email,
          wrongCode,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn("123456"); // 다른 코드

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.verifyCode(request);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("실패 - DB에 토큰이 존재하지 않음")
    void verifyCode_fail_token_not_found() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          email,
          code,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn(code);
      when(
          verificationTokenRepository.findTopByIdentifierAndVerificationTypeAndPurposeOrderByCreatedAtDesc(
              anyString(), any(), any()))
          .thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.verifyCode(request);
      });

      assertEquals("AUTH-401", exception.getCode());
    }

    @Test
    @DisplayName("실패 - 이미 사용된 인증 코드")
    void verifyCode_fail_already_used() {
      // Given
      String email = "test@test.com";
      String code = "123456";
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          email,
          code,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      VerificationToken token = VerificationToken.builder()
          .identifier(email)
          .verificationType(VerificationType.EMAIL)
          .purpose(VerificationPurpose.SIGNUP)
          .validMinutes(5)
          .build();
      ReflectionTestUtils.setField(token, "verificationCode", code);
      ReflectionTestUtils.setField(token, "isUsed", true); // 이미 사용됨
      ReflectionTestUtils.setField(token, "expireAt", LocalDateTime.now().plusMinutes(5));

      when(valueOperations.get(anyString())).thenReturn(code);
      when(
          verificationTokenRepository.findTopByIdentifierAndVerificationTypeAndPurposeOrderByCreatedAtDesc(
              email, VerificationType.EMAIL, VerificationPurpose.SIGNUP))
          .thenReturn(Optional.of(token));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.verifyCode(request);
      });

      assertEquals("AUTH-401", exception.getCode());
      verify(verificationTokenRepository, never()).save(any(VerificationToken.class));
    }
  }

  @Nested
  @DisplayName("Rate Limiting 테스트")
  class RateLimitingTest {

    @Test
    @DisplayName("성공 - 첫 번째 시도")
    void rateLimit_first_attempt() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn(null); // 첫 시도
      when(valueOperations.increment(anyString())).thenReturn(1L);
      when(verificationTokenRepository.save(any(VerificationToken.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

      // When & Then
      assertDoesNotThrow(() -> verificationService.sendVerificationCode(request));
      verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    @DisplayName("성공 - 4번째 시도 (제한 이내)")
    void rateLimit_fourth_attempt() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn("4"); // 4번째 시도
      when(valueOperations.increment(anyString())).thenReturn(5L);
      when(verificationTokenRepository.save(any(VerificationToken.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

      // When & Then
      assertDoesNotThrow(() -> verificationService.sendVerificationCode(request));
    }

    @Test
    @DisplayName("실패 - 5번째 시도에서 제한")
    void rateLimit_fifth_attempt_blocked() {
      // Given
      String email = "test@test.com";
      VerificationSendRequest request = new VerificationSendRequest(
          email,
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(valueOperations.get(anyString())).thenReturn("5"); // 이미 5회

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        verificationService.sendVerificationCode(request);
      });

      assertEquals("AUTH-429", exception.getCode());
      verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
  }
}