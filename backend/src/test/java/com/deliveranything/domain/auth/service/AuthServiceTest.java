package com.deliveranything.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.enums.SocialProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private AccessTokenService accessTokenService;

  @Mock
  private TokenBlacklistService tokenBlacklistService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthService authService;

  @Nested
  @DisplayName("회원가입 테스트")
  class SignupTest {

    @Test
    @DisplayName("성공 - 일반 회원가입")
    void signup_success() {
      // Given
      String email = "test@test.com";
      String password = "password123";
      String username = "테스트";
      String phoneNumber = "01012345678";

      when(userRepository.existsByEmail(email)).thenReturn(false);
      when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
      when(passwordEncoder.encode(password)).thenReturn("encoded-password");

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn(email);
      when(userRepository.save(any(User.class))).thenReturn(mockUser);

      // When
      User result = authService.signup(email, password, username, phoneNumber);

      // Then
      assertNotNull(result);
      assertEquals(1L, result.getId());
      assertEquals(email, result.getEmail());
      verify(userRepository, times(1)).existsByEmail(email);
      verify(userRepository, times(1)).existsByPhoneNumber(phoneNumber);
      verify(passwordEncoder, times(1)).encode(password);
      verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("실패 - 이메일 중복")
    void signup_fail_duplicate_email() {
      // Given
      String email = "duplicate@test.com";
      when(userRepository.existsByEmail(email)).thenReturn(true);

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.signup(email, "password", "name", "01012345678");
      });

      assertEquals("USER-409", exception.getCode());
      verify(userRepository, times(1)).existsByEmail(email);
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("실패 - 전화번호 중복")
    void signup_fail_duplicate_phone() {
      // Given
      String phoneNumber = "01012345678";
      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.signup("test@test.com", "password", "name", phoneNumber);
      });

      assertEquals("USER-409", exception.getCode());
      verify(userRepository, times(1)).existsByPhoneNumber(phoneNumber);
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("성공 - OAuth2 회원가입")
    void signupOAuth2_success() {
      // Given
      String email = "oauth@test.com";
      String username = "OAuth User";
      String socialId = "google-123456";

      when(userRepository.existsByEmail(email)).thenReturn(false);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(userRepository.save(any(User.class))).thenReturn(mockUser);

      // When
      User result = authService.signupOAuth2(email, username, SocialProvider.GOOGLE, socialId);

      // Then
      assertNotNull(result);
      verify(userRepository, times(1)).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("로그인 테스트")
  class LoginTest {

    @Test
    @DisplayName("성공 - 일반 로그인")
    void login_success() {
      // Given
      String email = "test@test.com";
      String password = "password123";
      String deviceInfo = "Chrome";

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getPassword()).thenReturn("encoded-password");
      when(mockUser.isEnabled()).thenReturn(true);

      when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
      when(passwordEncoder.matches(password, "encoded-password")).thenReturn(true);
      when(accessTokenService.genAccessToken(mockUser)).thenReturn("access-token");
      when(refreshTokenService.genRefreshToken(mockUser, deviceInfo)).thenReturn("refresh-token");

      // When
      AuthService.LoginResult result = authService.login(email, password, deviceInfo);

      // Then
      assertNotNull(result);
      assertEquals(mockUser, result.user());
      assertEquals("access-token", result.accessToken());
      assertEquals("refresh-token", result.refreshToken());
      verify(mockUser, times(1)).updateLastLoginAt();
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void login_fail_user_not_found() {
      // Given
      String email = "notfound@test.com";
      when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.login(email, "password", "device");
      });

      assertEquals("USER-404", exception.getCode());
      verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("실패 - 비밀번호 불일치")
    void login_fail_invalid_password() {
      // Given
      User mockUser = mock(User.class);
      when(mockUser.getPassword()).thenReturn("encoded-password");
      when(mockUser.isEnabled()).thenReturn(true);

      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
      when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.login("test@test.com", "wrong-password", "device");
      });

      assertEquals("USER-404", exception.getCode());
      verify(mockUser, never()).updateLastLoginAt();
    }

    @Test
    @DisplayName("실패 - 비활성화된 계정")
    void login_fail_disabled_account() {
      // Given
      User mockUser = mock(User.class);
      when(mockUser.isEnabled()).thenReturn(false);
      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.login("test@test.com", "password", "device");
      });

      assertEquals("USER-404", exception.getCode());
    }
  }

  @Nested
  @DisplayName("로그아웃 테스트")
  class LogoutTest {

    @Test
    @DisplayName("성공 - 단일 로그아웃")
    void logout_success() {
      // Given
      Long userId = 1L;
      String deviceInfo = "Chrome";
      String accessToken = "access-token";

      doNothing().when(refreshTokenService).invalidateRefreshToken(userId, deviceInfo);
      doNothing().when(tokenBlacklistService).addToBlacklist(accessToken);

      // When
      authService.logout(userId, deviceInfo, accessToken);

      // Then
      verify(refreshTokenService, times(1)).invalidateRefreshToken(userId, deviceInfo);
      verify(tokenBlacklistService, times(1)).addToBlacklist(accessToken);
    }

    @Test
    @DisplayName("성공 - 단일 로그아웃 (accessToken null)")
    void logout_success_without_token() {
      // Given
      Long userId = 1L;
      String deviceInfo = "Chrome";

      doNothing().when(refreshTokenService).invalidateRefreshToken(userId, deviceInfo);

      // When
      authService.logout(userId, deviceInfo, null);

      // Then
      verify(refreshTokenService, times(1)).invalidateRefreshToken(userId, deviceInfo);
      verify(tokenBlacklistService, never()).addToBlacklist(anyString());
    }

    @Test
    @DisplayName("성공 - 전체 로그아웃")
    void logoutAll_success() {
      // Given
      Long userId = 1L;
      String accessToken = "access-token";

      doNothing().when(refreshTokenService).invalidateAllRefreshTokens(userId);
      doNothing().when(tokenBlacklistService).addToBlacklist(accessToken);

      // When
      authService.logoutAll(userId, accessToken);

      // Then
      verify(refreshTokenService, times(1)).invalidateAllRefreshTokens(userId);
      verify(tokenBlacklistService, times(1)).addToBlacklist(accessToken);
    }
  }

  @Nested
  @DisplayName("이메일 인증 테스트")
  class VerifyEmailTest {

    @Test
    @DisplayName("성공 - 이메일 인증")
    void verifyEmail_success() {
      // Given
      Long userId = 1L;
      User mockUser = mock(User.class);

      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
      doNothing().when(mockUser).verifyEmail();
      when(userRepository.save(mockUser)).thenReturn(mockUser);

      // When
      authService.verifyEmail(userId);

      // Then
      verify(mockUser, times(1)).verifyEmail();
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void verifyEmail_fail_user_not_found() {
      // Given
      Long userId = 999L;
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      // When & Then
      CustomException exception = assertThrows(CustomException.class, () -> {
        authService.verifyEmail(userId);
      });

      assertEquals("USER-404", exception.getCode());
    }
  }

  @Nested
  @DisplayName("OAuth2 로그인 테스트")
  class OAuth2LoginTest {

    @Test
    @DisplayName("성공 - 기존 사용자 로그인")
    void oAuth2SignupOrLogin_existing_user() {
      // Given
      String email = "oauth@test.com";
      String username = "OAuth User";
      String socialId = "google-123";

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(userRepository.findBySocialProviderAndSocialId(SocialProvider.GOOGLE, socialId))
          .thenReturn(Optional.of(mockUser));
      doNothing().when(mockUser).updateSocialInfo(username, email);
      doNothing().when(mockUser).updateLastLoginAt();
      when(userRepository.save(mockUser)).thenReturn(mockUser);

      // When
      User result = authService.oAuth2SignupOrLogin(email, username, SocialProvider.GOOGLE,
          socialId);

      // Then
      assertNotNull(result);
      assertEquals(1L, result.getId());
      verify(mockUser, times(1)).updateSocialInfo(username, email);
      verify(mockUser, times(1)).updateLastLoginAt();
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("성공 - 신규 사용자 회원가입")
    void oAuth2SignupOrLogin_new_user() {
      // Given
      String email = "new@test.com";
      String username = "New User";
      String socialId = "google-456";

      when(userRepository.findBySocialProviderAndSocialId(SocialProvider.GOOGLE, socialId))
          .thenReturn(Optional.empty());
      when(userRepository.existsByEmail(email)).thenReturn(false);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(2L);
      when(userRepository.save(any(User.class))).thenReturn(mockUser);

      // When
      User result = authService.oAuth2SignupOrLogin(email, username, SocialProvider.GOOGLE,
          socialId);

      // Then
      assertNotNull(result);
      verify(userRepository, times(1)).save(any(User.class));
    }
  }
}