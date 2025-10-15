package com.deliveranything.global.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.auth.service.AccessTokenService;
import com.deliveranything.domain.auth.auth.service.TokenBlacklistService;
import com.deliveranything.domain.auth.auth.service.UserAuthorityProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.security.auth.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAuthenticationFilter 단위 테스트")
class CustomAuthenticationFilterTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccessTokenService accessTokenService;

  @Mock
  private UserAuthorityProvider userAuthorityProvider;

  @Mock
  private TokenBlacklistService tokenBlacklistService;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private CustomAuthenticationFilter filter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("인증 성공 테스트")
  class AuthenticationSuccessTest {

    @Test
    @DisplayName("성공 - 유효한 JWT 토큰")
    void doFilterInternal_valid_token() throws Exception {
      String token = "valid-jwt-token";
      request.addHeader("Authorization", "Bearer " + token);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getUsername()).thenReturn("테스트");
      when(mockUser.getEmail()).thenReturn("test@test.com");
      when(mockUser.getCurrentActiveProfile()).thenReturn(null);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(accessTokenService.isValidToken(token)).thenReturn(true);
      when(accessTokenService.isTokenExpired(token)).thenReturn(false);
      when(accessTokenService.payload(token)).thenReturn(Map.of("id", 1L));
      when(userRepository.findByIdWithProfile(1L)).thenReturn(Optional.of(mockUser));
      when(userAuthorityProvider.getAuthorities(mockUser))
          .thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNotNull(auth);

      SecurityUser principalUser = (SecurityUser) auth.getPrincipal();

      assertEquals("테스트", principalUser.getRealName());

      assertEquals("1", auth.getName());

      verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("성공 - Authorization 헤더 없음 (인증 건너뜀)")
    void doFilterInternal_no_authorization_header() throws Exception {
      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNull(auth);
      verify(filterChain, times(1)).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("인증 실패 테스트")
  class AuthenticationFailureTest {

    @Test
    @DisplayName("실패 - 블랙리스트 토큰")
    void doFilterInternal_blacklisted_token() throws Exception {
      String token = "blacklisted-token";
      request.addHeader("Authorization", "Bearer " + token);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);
      when(objectMapper.writeValueAsString(any())).thenReturn("{}");

      filter.doFilterInternal(request, response, filterChain);

      assertEquals(401, response.getStatus());
      verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("실패 - 만료된 토큰")
    void doFilterInternal_expired_token() throws Exception {
      String token = "expired-token";
      request.addHeader("Authorization", "Bearer " + token);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(accessTokenService.isValidToken(token)).thenReturn(true);
      when(accessTokenService.isTokenExpired(token)).thenReturn(true);

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNull(auth);
      verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("실패 - 유효하지 않은 토큰")
    void doFilterInternal_invalid_token() throws Exception {
      String token = "invalid-token";
      request.addHeader("Authorization", "Bearer " + token);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(accessTokenService.isValidToken(token)).thenReturn(false);

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNull(auth);
      verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void doFilterInternal_user_not_found() throws Exception {
      String token = "valid-token-but-user-not-exist";
      request.addHeader("Authorization", "Bearer " + token);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(accessTokenService.isValidToken(token)).thenReturn(true);
      when(accessTokenService.isTokenExpired(token)).thenReturn(false);
      when(accessTokenService.payload(token)).thenReturn(Map.of("id", 999L));
      when(userRepository.findByIdWithProfile(999L)).thenReturn(Optional.empty());

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNull(auth);
      verify(filterChain, times(1)).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("토큰 추출 테스트")
  class ExtractTokenTest {

    @Test
    @DisplayName("성공 - Bearer 토큰 추출")
    void extractAccessToken_with_bearer_prefix() throws Exception {
      String token = "test-token";
      request.addHeader("Authorization", "Bearer " + token);

      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(accessTokenService.isValidToken(token)).thenReturn(true);
      when(accessTokenService.isTokenExpired(token)).thenReturn(false);
      when(accessTokenService.payload(token)).thenReturn(Map.of("id", 1L));

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getUsername()).thenReturn("테스트");
      when(mockUser.getEmail()).thenReturn("test@test.com");

      when(userRepository.findByIdWithProfile(1L)).thenReturn(Optional.of(mockUser));
      when(userAuthorityProvider.getAuthorities(mockUser))
          .thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

      filter.doFilterInternal(request, response, filterChain);

      verify(accessTokenService, times(1)).payload(token);
    }

    @Test
    @DisplayName("실패 - Bearer 접두사 없음")
    void extractAccessToken_without_bearer_prefix() throws Exception {
      request.addHeader("Authorization", "invalid-format-token");

      filter.doFilterInternal(request, response, filterChain);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      assertNull(auth);
      verify(accessTokenService, never()).isValidToken(anyString());
    }
  }
}