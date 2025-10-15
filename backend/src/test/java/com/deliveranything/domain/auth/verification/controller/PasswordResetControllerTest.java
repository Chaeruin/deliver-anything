package com.deliveranything.domain.auth.verification.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveranything.domain.auth.verification.dto.PasswordResetConfirmRequest;
import com.deliveranything.domain.auth.verification.dto.PasswordResetRequest;
import com.deliveranything.domain.auth.verification.dto.PasswordResetVerifyRequest;
import com.deliveranything.domain.auth.verification.service.PasswordResetService;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetController 테스트")
class PasswordResetControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private PasswordResetService passwordResetService;

  @InjectMocks
  private PasswordResetController passwordResetController;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(passwordResetController)
        .setControllerAdvice(new com.deliveranything.global.exception.GlobalExceptionHandler())
        .setValidator(validator)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  @Nested
  @DisplayName("POST /api/v1/auth/password/reset/request - 비밀번호 재설정 요청")
  class RequestPasswordResetTest {

    @Test
    @DisplayName("성공 - 올바른 이메일로 인증 코드 발송")
    void requestPasswordReset_success() throws Exception {
      // Given
      PasswordResetRequest request = new PasswordResetRequest("test@test.com");

      doNothing().when(passwordResetService).requestPasswordReset(anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("인증 코드가 이메일로 발송되었습니다."));

      verify(passwordResetService, times(1)).requestPasswordReset("test@test.com");
    }

    @Test
    @DisplayName("실패 - 잘못된 이메일 형식")
    void requestPasswordReset_fail_invalid_email() throws Exception {
      // Given
      PasswordResetRequest request = new PasswordResetRequest("invalid-email");

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("INPUT-400"));
    }

    @Test
    @DisplayName("실패 - 빈 이메일")
    void requestPasswordReset_fail_empty_email() throws Exception {
      // Given
      PasswordResetRequest request = new PasswordResetRequest("");

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 이메일")
    void requestPasswordReset_fail_user_not_found() throws Exception {
      // Given
      PasswordResetRequest request = new PasswordResetRequest("notfound@test.com");

      doThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
          .when(passwordResetService).requestPasswordReset(anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("USER-404"));
    }

    @Test
    @DisplayName("실패 - 소셜 로그인 사용자")
    void requestPasswordReset_fail_social_user() throws Exception {
      // Given
      PasswordResetRequest request = new PasswordResetRequest("google@test.com");

      doThrow(new CustomException(ErrorCode.TOKEN_INVALID))
          .when(passwordResetService).requestPasswordReset(anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/password/reset/verify - 인증 코드 검증 및 토큰 발급")
  class VerifyCodeTest {

    @Test
    @DisplayName("성공 - 올바른 인증 코드로 토큰 발급")
    void verifyCode_success() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "test@test.com",
          "123456"
      );

      String resetToken = "valid-reset-token-uuid";
      when(passwordResetService.verifyCodeAndIssueResetToken(anyString(), anyString()))
          .thenReturn(resetToken);

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("인증이 완료되었습니다. 새 비밀번호를 설정해주세요."))
          .andExpect(jsonPath("$.content.resetToken").value(resetToken));

      verify(passwordResetService, times(1))
          .verifyCodeAndIssueResetToken("test@test.com", "123456");
    }

    @Test
    @DisplayName("실패 - 잘못된 인증 코드")
    void verifyCode_fail_invalid_code() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "test@test.com",
          "999999"
      );

      when(passwordResetService.verifyCodeAndIssueResetToken(anyString(), anyString()))
          .thenThrow(new CustomException(ErrorCode.TOKEN_INVALID));

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    @DisplayName("실패 - 만료된 인증 코드")
    void verifyCode_fail_expired() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "test@test.com",
          "123456"
      );

      when(passwordResetService.verifyCodeAndIssueResetToken(anyString(), anyString()))
          .thenThrow(new CustomException(ErrorCode.TOKEN_EXPIRED));

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    @DisplayName("실패 - 빈 이메일")
    void verifyCode_fail_empty_email() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "",
          "123456"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 빈 인증 코드")
    void verifyCode_fail_empty_code() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "test@test.com",
          ""
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 인증 코드 길이 부족")
    void verifyCode_fail_code_length_invalid() throws Exception {
      // Given
      PasswordResetVerifyRequest request = new PasswordResetVerifyRequest(
          "test@test.com",
          "12345"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/password/reset/confirm - 새 비밀번호 설정")
  class ConfirmPasswordResetTest {

    @Test
    @DisplayName("성공 - 유효한 토큰으로 비밀번호 변경")
    void confirmPasswordReset_success() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-reset-token",
          "NewPassword123!"
      );

      doNothing().when(passwordResetService).resetPassword(anyString(), anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));

      verify(passwordResetService, times(1))
          .resetPassword("valid-reset-token", "NewPassword123!");
    }

    @Test
    @DisplayName("실패 - 만료된 토큰")
    void confirmPasswordReset_fail_expired_token() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "expired-token",
          "NewPassword123!"
      );

      doThrow(new CustomException(ErrorCode.TOKEN_EXPIRED))
          .when(passwordResetService).resetPassword(anyString(), anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 토큰")
    void confirmPasswordReset_fail_invalid_token() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "invalid-token",
          "NewPassword123!"
      );

      doThrow(new CustomException(ErrorCode.TOKEN_EXPIRED))
          .when(passwordResetService).resetPassword(anyString(), anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    @DisplayName("실패 - 빈 토큰")
    void confirmPasswordReset_fail_empty_token() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "",
          "NewPassword123!"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 빈 비밀번호")
    void confirmPasswordReset_fail_empty_password() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          ""
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 비밀번호 길이 부족 (7자)")
    void confirmPasswordReset_fail_password_too_short() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          "Pass1!"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("INPUT-400"));
    }

    @Test
    @DisplayName("실패 - 영문 미포함")
    void confirmPasswordReset_fail_no_letter() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          "12345678!"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 숫자 미포함")
    void confirmPasswordReset_fail_no_digit() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          "Password!"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 특수문자 미포함")
    void confirmPasswordReset_fail_no_special_char() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          "Password123"
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void confirmPasswordReset_fail_user_not_found() throws Exception {
      // Given
      PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
          "valid-token",
          "NewPassword123!"
      );

      doThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
          .when(passwordResetService).resetPassword(anyString(), anyString());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("USER-404"));
    }
  }

  @Nested
  @DisplayName("통합 플로우 테스트")
  class IntegrationFlowTest {

    @Test
    @DisplayName("성공 - 전체 비밀번호 재설정 플로우")
    void full_password_reset_flow() throws Exception {
      String email = "test@test.com";
      String code = "123456";
      String resetToken = "valid-reset-token";
      String newPassword = "NewPassword123!";

      // 1단계: 비밀번호 재설정 요청
      PasswordResetRequest requestStep1 = new PasswordResetRequest(email);
      doNothing().when(passwordResetService).requestPasswordReset(email);

      mockMvc.perform(post("/api/v1/auth/password/reset/request")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestStep1)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      // 2단계: 인증 코드 검증 및 토큰 발급
      PasswordResetVerifyRequest requestStep2 = new PasswordResetVerifyRequest(email, code);
      when(passwordResetService.verifyCodeAndIssueResetToken(email, code))
          .thenReturn(resetToken);

      mockMvc.perform(post("/api/v1/auth/password/reset/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestStep2)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.content.resetToken").value(resetToken));

      // 3단계: 새 비밀번호 설정
      PasswordResetConfirmRequest requestStep3 = new PasswordResetConfirmRequest(
          resetToken, newPassword);
      doNothing().when(passwordResetService).resetPassword(resetToken, newPassword);

      mockMvc.perform(post("/api/v1/auth/password/reset/confirm")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestStep3)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다."));

      // 검증
      verify(passwordResetService, times(1)).requestPasswordReset(email);
      verify(passwordResetService, times(1)).verifyCodeAndIssueResetToken(email, code);
      verify(passwordResetService, times(1)).resetPassword(resetToken, newPassword);
    }
  }
}