package com.deliveranything.global.security.handler;

import com.deliveranything.domain.auth.service.TokenService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.security.auth.SecurityUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final Rq rq;
  private final TokenService tokenService;

  @Value("${custom.frontend.url}")
  private String frontendUrl;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException, ServletException {

    SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
    Long userId = securityUser.getId();

    // User 조회
    User actor = rq.getActor();

    // 토큰 발급
    String accessToken = tokenService.genAccessToken(actor);

    // 디바이스 ID 처리
    String deviceId = request.getHeader("X-Device-ID");
    if (deviceId == null || deviceId.isBlank()) {
      deviceId = UUID.randomUUID().toString();
      response.addHeader("X-Device-ID", deviceId);
      log.info("신규 OAuth2 기기, X-Device-ID 발급: {}", deviceId);
    }
    String refreshToken = tokenService.genRefreshToken(actor, deviceId);

    // 쿠키 설정
    rq.setAccessToken(accessToken);         // 헤더만
    rq.setRefreshToken(refreshToken);       // 쿠키만

    log.info("OAuth2_로그인_성공: userId={}", userId);

    // 프로필 존재 여부에 따라 리다이렉트
    if (!actor.hasActiveProfile()) {  // 메서드명 변경
      log.info("프로필이 없는 사용자, 프로필 생성 페이지로 이동: userId={}", userId);
      rq.sendRedirect(frontendUrl + "/make-profile");  // 현재 프론트 개발자님이 실제로 쓰고계신 경로 변경!
    } else {
      log.info("프로필이 있는 사용자, 메인 페이지로 이동: userId={}", userId);
      rq.sendRedirect(frontendUrl);
    }
  }
}
