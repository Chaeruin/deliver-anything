package com.deliveranything.domain.auth.verification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveranything.domain.auth.verification.dto.VerificationSendRequest;
import com.deliveranything.domain.auth.verification.dto.VerificationVerifyRequest;
import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import com.deliveranything.domain.auth.verification.service.VerificationService;
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
@DisplayName("VerificationController 테스트")
class VerificationControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private VerificationService verificationService;

  @InjectMocks
  private VerificationController verificationController;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(verificationController)
        .setControllerAdvice(new com.deliveranything.global.exception.GlobalExceptionHandler())
        .setValidator(validator)
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .build();
  }

  @Nested
  @DisplayName("POST /api/v1/auth/verification/send - 인증 코드 발송")
  class SendVerificationCodeTest {

    @Test
    @DisplayName("성공 - 이메일 인증 코드 발송")
    void sendVerificationCode_success() throws Exception {
      // Given
      VerificationSendRequest request = new VerificationSendRequest(
          "test@test.com",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      doNothing().when(verificationService).sendVerificationCode(any());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."))
          .andExpect(jsonPath("$.content").isEmpty());

      verify(verificationService, times(1)).sendVerificationCode(any());
    }

    @Test
    @DisplayName("실패 - 잘못된 이메일 형식")
    void sendVerificationCode_fail_invalid_email() throws Exception {
      // Given - @ 없는 명확히 잘못된 이메일
      VerificationSendRequest request = new VerificationSendRequest(
          "notanemail",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("INPUT-400"));
    }

    @Test
    @DisplayName("실패 - 빈 이메일")
    void sendVerificationCode_fail_empty_email() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": "",
            "verificationType": "EMAIL",
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("실패 - null 이메일")
    void sendVerificationCode_fail_null_email() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": null,
            "verificationType": "EMAIL",
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - verificationType null")
    void sendVerificationCode_fail_null_type() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": "test@test.com",
            "verificationType": null,
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - Rate Limit 초과")
    void sendVerificationCode_fail_rate_limit() throws Exception {
      // Given
      VerificationSendRequest request = new VerificationSendRequest(
          "test@test.com",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      doThrow(new CustomException(ErrorCode.TOKEN_REFRESH_RATE_LIMIT_EXCEEDED))
          .when(verificationService).sendVerificationCode(any());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isTooManyRequests())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-429"));
    }
    
    @Test
    @DisplayName("실패 - SMS 타입은 지원하지 않음")
    void sendVerificationCode_fail_sms_not_supported() throws Exception {
      // Given - 이메일 형식은 맞지만 PHONE 타입으로 요청
      VerificationSendRequest request = new VerificationSendRequest(
          "test@test.com",  // 이메일 형식으로 보내되 PHONE 타입으로 요청
          VerificationType.PHONE,
          VerificationPurpose.SIGNUP
      );

      doThrow(new CustomException(ErrorCode.TOKEN_INVALID))
          .when(verificationService).sendVerificationCode(any());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/send")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/verification/verify - 인증 코드 검증")
  class VerifyCodeTest {

    @Test
    @DisplayName("성공 - 올바른 인증 코드")
    void verifyCode_success() throws Exception {
      // Given
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          "test@test.com",
          "123456",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      when(verificationService.verifyCode(any())).thenReturn(true);

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("인증이 완료되었습니다."));

      verify(verificationService, times(1)).verifyCode(any());
    }

    @Test
    @DisplayName("실패 - 잘못된 인증 코드")
    void verifyCode_fail_invalid_code() throws Exception {
      // Given
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          "test@test.com",
          "999999",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      doThrow(new CustomException(ErrorCode.TOKEN_INVALID))
          .when(verificationService).verifyCode(any());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
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
      VerificationVerifyRequest request = new VerificationVerifyRequest(
          "test@test.com",
          "123456",
          VerificationType.EMAIL,
          VerificationPurpose.SIGNUP
      );

      doThrow(new CustomException(ErrorCode.TOKEN_EXPIRED))
          .when(verificationService).verifyCode(any());

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    @DisplayName("실패 - 인증 코드 길이 부족 (5자리)")
    void verifyCode_fail_code_too_short() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": "test@test.com",
            "verificationCode": "12345",
            "verificationType": "EMAIL",
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("실패 - 인증 코드 길이 초과 (7자리)")
    void verifyCode_fail_code_too_long() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": "test@test.com",
            "verificationCode": "1234567",
            "verificationType": "EMAIL",
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("실패 - 빈 인증 코드")
    void verifyCode_fail_empty_code() throws Exception {
      // Given
      String requestJson = """
          {
            "identifier": "test@test.com",
            "verificationCode": "",
            "verificationType": "EMAIL",
            "purpose": "SIGNUP"
          }
          """;

      // When & Then
      mockMvc.perform(post("/api/v1/auth/verification/verify")
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson))
          .andExpect(status().isBadRequest());
    }
  }
}