package com.deliveranything.global.security.filter;

import com.deliveranything.domain.auth.service.AuthTokenService;
import com.deliveranything.domain.auth.service.TokenBlacklistService;
import com.deliveranything.domain.auth.service.UserAuthorityProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;
  private final AuthTokenService authTokenService;
  private final UserAuthorityProvider userAuthorityProvider;
  private final TokenBlacklistService tokenBlacklistService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws IOException {

    try {
      processAuthentication(request, response, filterChain);
    } catch (CustomException e) {
      handleAuthenticationError(response, e);
    } catch (ServletException e) {
      handleAuthenticationError(response, new CustomException(ErrorCode.TOKEN_INVALID));
    } catch (Exception e) {
      handleAuthenticationError(response, new CustomException(ErrorCode.USER_NOT_FOUND));
    }
  }

  private void processAuthentication(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String accessToken = extractAccessToken(request);

    if (StringUtils.hasText(accessToken)) {
      User user = authenticateWithAccessToken(accessToken);
      if (user != null) {
        setAuthentication(user);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractAccessToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
      return authorization.substring(7);
    }
    return null;
  }

  private User authenticateWithAccessToken(String accessToken) {

    // 블랙리스트 체크
    if (tokenBlacklistService.isBlacklisted(accessToken)) {
      throw new CustomException(ErrorCode.TOKEN_INVALID);
    }

    if (!authTokenService.isValidToken(accessToken) || authTokenService.isTokenExpired(
        accessToken)) {
      return null;
    }

    Map<String, Object> payload = authTokenService.payload(accessToken);
    if (payload == null) {
      return null;
    }

    Long userId = Long.parseLong(String.valueOf(payload.get("id")));
    return userRepository.findByIdWithProfile(userId).orElse(null);
  }

  private void setAuthentication(User user) {
    Collection<? extends GrantedAuthority> authorities = userAuthorityProvider.getAuthorities(user);

    UserDetails securityUser = new SecurityUser(
        user.getId(),
        user.getUsername(),
        "",
        user.getEmail(),
        user.getCurrentActiveProfile(),
        authorities
    );

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        securityUser,
        null,
        securityUser.getAuthorities()
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void handleAuthenticationError(HttpServletResponse response, CustomException e)
      throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(e.getHttpStatus().value());
    response.getWriter().write(objectMapper.writeValueAsString(
        com.deliveranything.global.common.ApiResponse.fail(e.getCode(), e.getMessage())
    ));
  }
}