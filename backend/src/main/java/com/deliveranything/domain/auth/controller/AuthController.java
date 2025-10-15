package com.deliveranything.domain.auth.controller;

import com.deliveranything.domain.auth.dto.LoginRequest;
import com.deliveranything.domain.auth.dto.LoginResponse;
import com.deliveranything.domain.auth.dto.SignupRequest;
import com.deliveranything.domain.auth.dto.SignupResponse;
import com.deliveranything.domain.auth.service.AuthService;
import com.deliveranything.domain.auth.service.RefreshTokenService;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.service.ProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증/인가 API", description = "auth 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final ProfileService profileService;
  private final Rq rq;

  @PostMapping("/signup")
  @Operation(
      summary = "회원가입",
      description = "이메일과 비밀번호로 신규 회원가입을 진행합니다. 비밀번호는 8자 이상, 영문/숫자/특수문자 포함이어야 합니다."
  )
  public ResponseEntity<ApiResponse<SignupResponse>> signup(
      @Valid @RequestBody SignupRequest request) {
    User user = authService.signup(
        request.email(),
        request.password(),
        request.name(),
        request.phoneNumber()
    );

    SignupResponse response = SignupResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .build();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
  }

  @PostMapping("/login")
  @Operation(
      summary = "로그인",
      description = "이메일과 비밀번호로 로그인합니다. Access Token과 Refresh Token이 발급됩니다. X-Device-ID 헤더가 없을 경우, 서버에서 생성하여 응답 헤더로 반환합니다."
  )
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      @RequestHeader(value = "X-Device-ID", required = false) String deviceId,
      HttpServletResponse httpResponse) {

    String finalDeviceId = deviceId;
    if (finalDeviceId == null || finalDeviceId.isBlank()) {
      finalDeviceId = UUID.randomUUID().toString();
      httpResponse.addHeader("X-Device-ID", finalDeviceId);
      log.info("신규 기기, X-Device-ID 발급: {}", finalDeviceId);
    }

    log.info("로그인 시도 - 기기 ID: {}", finalDeviceId);

    httpResponse.addHeader("X-Device-ID", finalDeviceId);

    // 로그인 처리 (storeId + 프로필 상세 정보 포함)
    AuthService.LoginResult result = authService.login(
        request.email(),
        request.password(),
        finalDeviceId
    );

    User user = result.user();
    List<ProfileType> availableProfiles = profileService.getAvailableProfiles(user);

    // storeId + 프로필 상세 정보 포함 응답 생성
    LoginResponse response = LoginResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .currentActiveProfileType(user.getCurrentActiveProfileType())
        .currentActiveProfileId(user.getCurrentActiveProfileId())
        .availableProfiles(availableProfiles)
        .storeId(result.storeId())  // 상점 ID
        .currentProfileDetail(result.currentProfileDetail())  // 프로필 상세 정보
        .build();

    // 토큰 설정 (쿠키 + 헤더)
    rq.setAccessToken(result.accessToken());      // 헤더만
    rq.setRefreshToken(result.refreshToken());    // 쿠키만

    return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
  }

  /**
   * 단일 로그아웃 (현재 기기)
   */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @AuthenticationPrincipal SecurityUser securityUser,
      @RequestHeader("Authorization") String authorization,
      @RequestHeader(value = "User-Agent", required = false) String userAgent
  ) {
    // Bearer 접두사 제거
    String accessToken = authorization.replace("Bearer ", "");

    String deviceInfo = userAgent != null ? userAgent : "unknown";

    //  accessToken 파라미터 전달
    authService.logout(securityUser.getId(), deviceInfo, accessToken);

    // 쿠키 삭제
    rq.deleteRefreshToken();

    log.info("로그아웃 완료: userId={}, deviceInfo={}", securityUser.getId(), deviceInfo);

    return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다.", null));
  }

  /**
   * 전체 로그아웃 (모든 기기)
   */
  @PostMapping("/logout/all")
  public ResponseEntity<ApiResponse<Void>> logoutAll(
      @AuthenticationPrincipal SecurityUser securityUser,
      @RequestHeader("Authorization") String authorization
  ) {
    String accessToken = authorization.replace("Bearer ", "");

    authService.logoutAll(securityUser.getId(), accessToken);

    // 쿠키 삭제
    rq.deleteRefreshToken();

    log.info("전체 로그아웃 완료: userId={}", securityUser.getId());

    return ResponseEntity.ok(ApiResponse.success());
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "토큰 재발급",
      description = "HttpOnly 쿠키에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다."
  )
  public ResponseEntity<ApiResponse<Void>> refreshToken() {

    log.info("Access Token 재발급 요청");

    // HttpOnly 쿠키에서 Refresh Token 추출
    String refreshToken = rq.getRefreshTokenFromCookie();

    if (refreshToken == null || refreshToken.isBlank()) {
      log.warn("Refresh Token이 쿠키에 존재하지 않습니다.");
      throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    String oldAccessToken = rq.getAccessTokenFromHeader();
    String newAccessToken = refreshTokenService.refreshAccessToken(
        refreshToken,
        oldAccessToken
    );

    // 쿠키 + 응답 헤더에도 설정
    rq.setAccessToken(newAccessToken);

    return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", null));
  }
}