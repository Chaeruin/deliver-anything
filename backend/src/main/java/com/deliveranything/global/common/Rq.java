package com.deliveranything.global.common;

import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Rq {

  private final HttpServletRequest req;
  private final HttpServletResponse resp;
  private final UserRepository userRepository;

  @Value("${custom.cookie.domain}")
  private String cookieDomain;

  @Value("${custom.refreshToken.expirationDays}")
  private int refreshTokenExpirationDays;

  // =====================================================================
  // 👤 사용자 관련
  // =====================================================================

  /**
   * 현재 SecurityUser 반환 (인증 객체 기반)
   */
  public SecurityUser getSecurityUser() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(SecurityUser.class::isInstance)
        .map(SecurityUser.class::cast)
        .orElse(null);
  }

  /**
   * 현재 사용자 ID 반환 (없으면 null)
   */
  public Long getActorId() {
    return Optional.ofNullable(getSecurityUser())
        .map(SecurityUser::getId)
        .orElse(null);
  }

  /**
   * 현재 로그인한 User 반환 (없으면 예외)
   */
  public User getActor() {
    Long userId = getActorId();
    if (userId == null) {
      throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
    }
    return userRepository.findByIdWithProfile(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
  }

  /**
   * 로그인 여부 확인
   */
  public boolean isAuthenticated() {
    return getActorId() != null;
  }

  /**
   * 관리자 여부 확인
   */
  public boolean isAdmin() {
    return Optional.ofNullable(getActor())
        .map(User::isAdmin)
        .orElse(false);
  }

  // =====================================================================
  // 🧭 프로필 관련
  // =====================================================================

  /**
   * 현재 활성화된 프로필 반환 (없으면 null)
   */
  public Profile getCurrentProfile() {
    return Optional.ofNullable(getSecurityUser())
        .map(SecurityUser::getCurrentActiveProfile)
        .orElse(null);
  }

  /**
   * 현재 활성 프로필 ID 반환 (null-safe)
   */
  public Long getCurrentProfileId() {
    return Optional.ofNullable(getCurrentProfile())
        .map(Profile::getId)
        .orElse(null);
  }

  /**
   * 현재 활성 프로필 타입이 특정 타입인지 확인
   */
  public boolean hasActiveProfile(ProfileType profileType) {
    return Optional.ofNullable(getCurrentProfile())
        .map(Profile::getType)
        .filter(type -> type == profileType)
        .isPresent();
  }

  // 각 프로필 타입별 활성화 여부
  public boolean isCustomerActive() {
    return hasActiveProfile(ProfileType.CUSTOMER);
  }

  public boolean isSellerActive() {
    return hasActiveProfile(ProfileType.SELLER);
  }

  public boolean isRiderActive() {
    return hasActiveProfile(ProfileType.RIDER);
  }

  // =====================================================================
  // 📦 HTTP 헤더 & 쿠키
  // =====================================================================

  /**
   * 요청 헤더 조회 (기본값 지원)
   */
  public String getHeader(String name, String defaultValue) {
    return Optional.ofNullable(req.getHeader(name))
        .filter(value -> !value.isBlank())
        .orElse(defaultValue);
  }

  /**
   * 응답 헤더 설정 (빈 값이면 무시)
   */
  public void setHeader(String name, String value) {
    if (value != null && !value.isBlank()) {
      resp.setHeader(name, value);
    }
  }

  /**
   * 쿠키 값 조회 (기본값 지원)
   */
  public String getCookieValue(String name, String defaultValue) {
    return Optional.ofNullable(req.getCookies())
        .flatMap(cookies -> Arrays.stream(cookies)
            .filter(c -> c.getName().equals(name))
            .map(Cookie::getValue)
            .filter(v -> !v.isBlank())
            .findFirst())
        .orElse(defaultValue);
  }

  /**
   * 쿠키 설정 (빈 값이면 삭제)
   */
  public void setCookie(String name, String value) {
    Cookie cookie = new Cookie(name, value != null ? value : "");
    cookie.setPath("/");
    cookie.setHttpOnly(true);

    if (!cookieDomain.equals("localhost")) {
      cookie.setSecure(true);
      cookie.setDomain(cookieDomain);
      cookie.setAttribute("SameSite", "None");
    } else {
      cookie.setDomain("localhost");
      cookie.setSecure(false);
      cookie.setAttribute("SameSite", "Lax");
    }

    int maxAge = (value == null || value.isBlank())
        ? 0
        : refreshTokenExpirationDays * 24 * 60 * 60;

    cookie.setMaxAge(maxAge);
    resp.addCookie(cookie);
  }

  /**
   * 쿠키 삭제
   */
  public void deleteCookie(String name) {
    setCookie(name, "");
  }

  /**
   * RefreshToken 쿠키에서 추출
   */
  public String getRefreshTokenFromCookie() {
    return getCookieValue("refreshToken", null);
  }

  // =====================================================================
  // 🔐 JWT 토큰 관련
  // =====================================================================

  /**
   * Authorization 헤더에서 AccessToken 추출
   */
  public String getAccessTokenFromHeader() {
    String authorization = getHeader("Authorization", "");
    return authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
  }

  /**
   * Authorization 헤더 설정
   */
  public void setAccessToken(String accessToken) {
    setHeader("Authorization", "Bearer " + accessToken);
  }

  /**
   * RefreshToken 쿠키 설정
   */
  public void setRefreshToken(String refreshToken) {
    setCookie("refreshToken", refreshToken);
  }

  /**
   * RefreshToken 쿠키 삭제
   */
  public void deleteRefreshToken() {
    deleteCookie("refreshToken");
  }

  // =====================================================================
  // 🚀 기타
  // =====================================================================

  /**
   * 지정된 URL로 리다이렉트
   */
  @SneakyThrows
  public void sendRedirect(String url) {
    resp.sendRedirect(url);
  }
}