package com.deliveranything.domain.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveranything.domain.auth.service.AuthService;
import com.deliveranything.domain.auth.service.RefreshTokenService;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.service.ProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.exception.GlobalExceptionHandler;
import com.deliveranything.global.security.auth.SecurityUser;
import com.deliveranything.global.util.UserAgentUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AuthService authService;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private ProfileService profileService;

  @Mock
  private UserAgentUtil userAgentUtil;

  @Mock
  private Rq rq;

  @InjectMocks
  private AuthController authController;

  private ObjectMapper objectMapper;

  private SecurityUser createMockSecurityUser(Long userId) {
    SecurityUser mockSecurityUser = mock(SecurityUser.class);
    when(mockSecurityUser.getId()).thenReturn(userId);
    when(mockSecurityUser.getAuthorities()).thenReturn(List.of());
    return mockSecurityUser;
  }

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders
        .standaloneSetup(authController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Nested
  @DisplayName("회원가입 API")
  class SignupTest {

    @Test
    @DisplayName("성공 - 201 Created")
    void signup_success() throws Exception {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn("test@test.com");
      when(mockUser.getUsername()).thenReturn("테스트");

      when(authService.signup(anyString(), anyString(), anyString(), anyString()))
          .thenReturn(mockUser);

      mockMvc.perform(post("/api/v1/auth/signup")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                      "email": "test@test.com",
                      "password": "!qwer1234",
                      "name": "테스트",
                      "phoneNumber": "01012345678"
                  }
                  """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
          .andExpect(jsonPath("$.content.userId").value(1))
          .andExpect(jsonPath("$.content.email").value("test@test.com"))
          .andExpect(jsonPath("$.content.username").value("테스트"));

      verify(authService, times(1))
          .signup("test@test.com", "!qwer1234", "테스트", "01012345678");
    }

    @Test
    @DisplayName("실패 - 이메일 중복")
    void signup_fail_duplicate_email() throws Exception {
      when(authService.signup(anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new CustomException(ErrorCode.USER_EMAIL_ALREADY_EXIST));

      String expectedMessage = ErrorCode.USER_EMAIL_ALREADY_EXIST.getMessage(); // "이미 존재하는 이메일 입니다."

      mockMvc.perform(post("/api/v1/auth/signup")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                      "email": "duplicate@test.com",
                      "password": "!qwer1234",
                      "name": "테스트",
                      "phoneNumber": "01012345678"
                  }
                  """))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("USER-409"))
          .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("실패 - 전화번호 중복")
    void signup_fail_duplicate_phone() throws Exception {
      when(authService.signup(anyString(), anyString(), anyString(), anyString()))
          .thenThrow(new CustomException(ErrorCode.USER_PHONE_ALREADY_EXIST));

      String expectedMessage = ErrorCode.USER_PHONE_ALREADY_EXIST.getMessage();

      mockMvc.perform(post("/api/v1/auth/signup")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                      "email": "test@test.com",
                      "password": "!qwer1234",
                      "name": "테스트",
                      "phoneNumber": "01012345678"
                  }
                  """))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("USER-409"))
          .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("실패 - 유효성 검사 실패 (빈 이메일)")
    void signup_fail_validation() throws Exception {
      mockMvc.perform(post("/api/v1/auth/signup")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                      "email": "",
                      "password": "!qwer1234",
                      "name": "테스트",
                      "phoneNumber": "01012345678"
                  }
                  """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("INPUT-400"))
          .andExpect(jsonPath("$.message").exists()); // 구체적인 메시지는 DTO에 따라 다름
    }
  }

  @Nested
  @DisplayName("로그인 API")
  class LoginTest {

    @Test
    @DisplayName("성공 - 프로필 없는 경우")
    void login_success_without_profile() throws Exception {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn("test@test.com");
      when(mockUser.getUsername()).thenReturn("테스트");
      when(mockUser.getCurrentActiveProfileType()).thenReturn(null);
      when(mockUser.getCurrentActiveProfileId()).thenReturn(null);

      AuthService.LoginResult loginResult = new AuthService.LoginResult(
          mockUser,
          "mock-access-token",
          "mock-refresh-token",
          null,
          null
      );

      when(authService.login(anyString(), anyString(), anyString()))
          .thenReturn(loginResult);
      when(profileService.getAvailableProfiles(any())).thenReturn(List.of());

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-Device-ID", "test-device-id")
              .content("""
                  {
                      "email": "test@test.com",
                      "password": "!qwer1234"
                  }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.message").value("로그인이 완료되었습니다."))
          .andExpect(jsonPath("$.content.userId").value(1))
          .andExpect(jsonPath("$.content.email").value("test@test.com"))
          .andExpect(jsonPath("$.content.currentActiveProfileType").isEmpty())
          .andExpect(jsonPath("$.content.currentActiveProfileId").isEmpty());
    }

    @Test
    @DisplayName("성공 - CUSTOMER 프로필 있는 경우")
    void login_success_with_customer_profile() throws Exception {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn("test@test.com");
      when(mockUser.getUsername()).thenReturn("테스트");
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);
      when(mockUser.getCurrentActiveProfileId()).thenReturn(10L);

      AuthService.LoginResult loginResult = new AuthService.LoginResult(
          mockUser,
          "mock-access-token",
          "mock-refresh-token",
          null,
          null
      );

      when(authService.login(anyString(), anyString(), anyString()))
          .thenReturn(loginResult);
      when(profileService.getAvailableProfiles(any()))
          .thenReturn(List.of(ProfileType.CUSTOMER));

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-Device-ID", "test-device-id")
              .content("""
                  {
                      "email": "test@test.com",
                      "password": "!qwer1234"
                  }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.content.currentActiveProfileType").value("CUSTOMER"))
          .andExpect(jsonPath("$.content.currentActiveProfileId").value(10))
          .andExpect(jsonPath("$.content.availableProfiles[0]").value("CUSTOMER"));
    }

    @Test
    @DisplayName("성공 - SELLER 프로필 + storeId 포함")
    void login_success_with_seller_profile_and_store() throws Exception {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn("seller@test.com");
      when(mockUser.getUsername()).thenReturn("판매자");
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.SELLER);
      when(mockUser.getCurrentActiveProfileId()).thenReturn(20L);

      AuthService.LoginResult loginResult = new AuthService.LoginResult(
          mockUser,
          "mock-access-token",
          "mock-refresh-token",
          100L,
          null
      );

      when(authService.login(anyString(), anyString(), anyString()))
          .thenReturn(loginResult);
      when(profileService.getAvailableProfiles(any()))
          .thenReturn(List.of(ProfileType.SELLER));

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-Device-ID", "test-device-id")
              .content("""
                  {
                      "email": "seller@test.com",
                      "password": "!qwer1234"
                  }
                  """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.content.storeId").value(100));
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void login_fail_user_not_found() throws Exception {
      String expectedMessage = ErrorCode.USER_NOT_FOUND.getMessage();

      when(authService.login(anyString(), anyString(), anyString()))
          .thenThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-Device-ID", "test-device-id")
              .content("""
                  {
                      "email": "notfound@test.com",
                      "password": "!qwer1234"
                  }
                  """))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("USER-404"))
          .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("실패 - 유효성 검사 실패 (빈 비밀번호)")
    void login_fail_validation() throws Exception {
      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                      "email": "test@test.com",
                      "password": ""
                  }
                  """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.code").value("INPUT-400"))
          .andExpect(jsonPath("$.message").exists());
    }
  }

  @Nested
  @DisplayName("로그아웃 API")
  class LogoutTest {

    @Test
    @DisplayName("단일 로그아웃 성공")
    void logout_success() {
      SecurityUser mockSecurityUser = mock(SecurityUser.class);
      when(mockSecurityUser.getId()).thenReturn(1L);

      String authorization = "Bearer mock-access-token";
      String userAgent = "Mozilla/5.0";

      doNothing().when(authService).logout(anyLong(), anyString(), anyString());

      ResponseEntity<?> response = authController.logout(
          mockSecurityUser,
          authorization,
          userAgent
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("로그아웃이 완료되었습니다.", body.getMessage());

      verify(authService, times(1)).logout(1L, userAgent, "mock-access-token");
    }

    @Test
    @DisplayName("단일 로그아웃 성공 - User-Agent 없음")
    void logout_without_user_agent() {
      SecurityUser mockSecurityUser = mock(SecurityUser.class);
      when(mockSecurityUser.getId()).thenReturn(1L);

      String authorization = "Bearer mock-access-token";

      doNothing().when(authService).logout(anyLong(), anyString(), anyString());

      ResponseEntity<?> response = authController.logout(
          mockSecurityUser,
          authorization,
          null
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());
      verify(authService, times(1)).logout(1L, "unknown", "mock-access-token");
    }

    @Test
    @DisplayName("전체 로그아웃 성공")
    void logout_all_success() {
      SecurityUser mockSecurityUser = mock(SecurityUser.class);
      when(mockSecurityUser.getId()).thenReturn(1L);

      String authorization = "Bearer mock-access-token";

      doNothing().when(authService).logoutAll(anyLong(), anyString());

      ResponseEntity<?> response = authController.logoutAll(
          mockSecurityUser,
          authorization
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(authService, times(1)).logoutAll(1L, "mock-access-token");
    }
  }

  @Nested
  @DisplayName("토큰 갱신 API")
  class RefreshTokenTest {

    @Test
    @DisplayName("성공 - Access Token 재발급")
    void refresh_token_success() {
      when(rq.getRefreshTokenFromCookie()).thenReturn("mock-refresh-token");
      when(rq.getAccessTokenFromHeader()).thenReturn("old-access-token");
      when(refreshTokenService.refreshAccessToken(anyString(), anyString()))
          .thenReturn("new-access-token");

      ResponseEntity<?> response = authController.refreshToken();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(rq, times(1)).getRefreshTokenFromCookie();
      verify(rq, times(1)).getAccessTokenFromHeader();
      verify(refreshTokenService, times(1))
          .refreshAccessToken("mock-refresh-token", "old-access-token");
      verify(rq, times(1)).setAccessToken("new-access-token");
    }

    @Test
    @DisplayName("실패 - 만료된 Refresh Token")
    void refresh_token_fail_expired() {
      when(rq.getRefreshTokenFromCookie()).thenReturn("expired-token");
      when(rq.getAccessTokenFromHeader()).thenReturn(null);
      when(refreshTokenService.refreshAccessToken(anyString(), isNull()))
          .thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

      CustomException exception = assertThrows(CustomException.class, () -> {
        authController.refreshToken();
      });

      assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - Refresh Token이 null인 경우")
    void refresh_token_fail_null() {
      when(rq.getRefreshTokenFromCookie()).thenReturn(null);

      CustomException exception = assertThrows(CustomException.class, () -> {
        authController.refreshToken();
      });

      assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 유효하지 않은 Refresh Token")
    void refresh_token_fail_invalid() {
      when(rq.getRefreshTokenFromCookie()).thenReturn("invalid-token");
      when(rq.getAccessTokenFromHeader()).thenReturn(null);
      when(refreshTokenService.refreshAccessToken(anyString(), isNull()))
          .thenThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

      CustomException exception = assertThrows(CustomException.class, () -> {
        authController.refreshToken();
      });

      assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }
  }
}