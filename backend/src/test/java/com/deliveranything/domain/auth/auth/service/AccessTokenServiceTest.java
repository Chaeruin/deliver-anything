package com.deliveranything.domain.auth.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.user.entity.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessTokenService 단위 테스트")
class AccessTokenServiceTest {

  @InjectMocks
  private AccessTokenService accessTokenService;

  @BeforeEach
  void setUp() {
    // private 필드 주입
    ReflectionTestUtils.setField(accessTokenService, "jwtSecretKey",
        "test-secret-key-must-be-at-least-32-characters-long-for-hs256");
    ReflectionTestUtils.setField(accessTokenService, "accessTokenExpirationSeconds", 3600);
  }

  @Nested
  @DisplayName("Access Token 생성 테스트")
  class GenAccessTokenTest {

    @Test
    @DisplayName("성공 - 프로필 없는 사용자")
    void genAccessToken_without_profile() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 1L);

      // When
      String token = accessTokenService.genAccessToken(user);
      Map<String, Object> payload = accessTokenService.payload(token); // 토큰 파싱

      // Then
      assertNotNull(token);
      assertTrue(token.length() > 0);
      assertNotNull(payload);
    }

    @Test
    @DisplayName("성공 - 프로필 있는 사용자")
    void genAccessToken_with_profile() {
      // Given
      // 1. Mock Profile 객체 생성 및 스텁 설정
      Profile mockProfile = mock(Profile.class);
      when(mockProfile.getType()).thenReturn(ProfileType.SELLER);
      when(mockProfile.getId()).thenReturn(100L); // Profile 엔티티의 ID는 100L 가정

      // 2. Mock User 객체 생성 및 스텁 설정 (Builder 대신 Mock 사용)
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getUsername()).thenReturn("테스트");

      when(mockUser.getCurrentActiveProfile()).thenReturn(mockProfile);
      // When
      String token = accessTokenService.genAccessToken(mockUser);
      Map<String, Object> payload = accessTokenService.payload(token); // 토큰 파싱

      // Then
      assertNotNull(token);
      assertTrue(token.length() > 0);
      assertNotNull(payload);

      assertTrue(payload.containsKey("currentActiveProfile"));
      assertTrue(payload.containsKey("currentActiveProfileId"));
      assertEquals("SELLER", payload.get("currentActiveProfile"));
      assertEquals(100L, payload.get("currentActiveProfileId"));
    }
  }

  @Nested
  @DisplayName("Token 파싱 테스트")
  class PayloadTest {

    @Test
    @DisplayName("성공 - 유효한 토큰 파싱")
    void payload_success() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 1L);

      String token = accessTokenService.genAccessToken(user);

      // When
      Map<String, Object> payload = accessTokenService.payload(token);

      // Then
      assertNotNull(payload);
      assertEquals(1L, payload.get("id"));
      assertEquals("테스트", payload.get("name"));
    }

    @Test
    @DisplayName("실패 - 잘못된 토큰")
    void payload_fail_invalid_token() {
      // Given
      String invalidToken = "invalid.token.here";

      // When
      Map<String, Object> payload = accessTokenService.payload(invalidToken);

      // Then
      assertNull(payload);
    }
  }

  @Nested
  @DisplayName("Token 검증 테스트")
  class ValidateTokenTest {

    @Test
    @DisplayName("성공 - 유효한 토큰")
    void isValidToken_success() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 1L);

      String token = accessTokenService.genAccessToken(user);

      // When
      boolean isValid = accessTokenService.isValidToken(token);

      // Then
      assertTrue(isValid);
    }

    @Test
    @DisplayName("실패 - 잘못된 토큰")
    void isValidToken_fail() {
      // Given
      String invalidToken = "invalid.token.here";

      // When
      boolean isValid = accessTokenService.isValidToken(invalidToken);

      // Then
      assertFalse(isValid);
    }

    @Test
    @DisplayName("성공 - 토큰 만료 확인 (만료되지 않음)")
    void isTokenExpired_not_expired() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 1L);

      String token = accessTokenService.genAccessToken(user);

      // When
      boolean isExpired = accessTokenService.isTokenExpired(token);

      // Then
      assertFalse(isExpired);
    }
  }

  @Nested
  @DisplayName("Token 정보 추출 테스트")
  class ExtractInfoTest {

    @Test
    @DisplayName("성공 - userId 추출")
    void getUserId_success() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 123L);

      String token = accessTokenService.genAccessToken(user);

      // When
      Long userId = accessTokenService.getUserId(token);

      // Then
      assertEquals(123L, userId);
    }

    @Test
    @DisplayName("성공 - 만료 시간 추출")
    void getExpirationTime_success() {
      // Given
      User user = User.builder()
          .email("test@test.com")
          .username("테스트")
          .password("encoded-password")
          .build();

      ReflectionTestUtils.setField(user, "id", 1L);

      String token = accessTokenService.genAccessToken(user);

      // When
      long expirationTime = accessTokenService.getExpirationTime(token);

      // Then
      assertTrue(expirationTime > System.currentTimeMillis());
    }
  }
}