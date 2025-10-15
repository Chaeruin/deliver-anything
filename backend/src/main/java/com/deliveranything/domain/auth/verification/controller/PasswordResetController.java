package com.deliveranything.domain.auth.verification.controller;

import com.deliveranything.domain.auth.verification.dto.PasswordResetConfirmRequest;
import com.deliveranything.domain.auth.verification.dto.PasswordResetRequest;
import com.deliveranything.domain.auth.verification.dto.PasswordResetVerifyRequest;
import com.deliveranything.domain.auth.verification.dto.PasswordResetVerifyResponse;
import com.deliveranything.domain.auth.verification.service.PasswordResetService;
import com.deliveranything.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 전용 3단계 프로세스를 처리하는 컨트롤러입니다. 기본 경로: /api/v1/auth/password
 */
@Tag(name = "비밀번호 재설정 API", description = "비밀번호 찾기 및 재설정")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  /**
   * 1단계: 재설정용 인증 코드 발송 (/api/v1/auth/password/reset/request)
   */
  @PostMapping("/reset/request")
  @Operation(
      summary = "1단계: 비밀번호 재설정 요청 (인증 코드 발송)",
      description = "사용자 이메일로 비밀번호 재설정용 인증 코드를 발송합니다. (VerificationPurpose.PASSWORD_RESET 사용)"
  )
  public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request
  ) {
    log.info("비밀번호 재설정 요청: email={}", request.email());

    passwordResetService.requestPasswordReset(request.email());

    return ResponseEntity.ok(
        ApiResponse.success("인증 코드가 이메일로 발송되었습니다.", null)
    );
  }

  /**
   * 2단계: 인증 코드 검증 및 재설정 토큰 발급 (/api/v1/auth/password/reset/verify)
   */
  @PostMapping("/reset/verify")
  @Operation(
      summary = "2단계: 인증 코드 검증 및 재설정 토큰 발급",
      description = "인증 코드를 검증하고 비밀번호 재설정용 `resetToken`을 발급받습니다."
  )
  public ResponseEntity<ApiResponse<PasswordResetVerifyResponse>> verifyCode(
      @Valid @RequestBody PasswordResetVerifyRequest request
  ) {
    log.info("인증 코드 검증 요청: email={}", request.email());

    String resetToken = passwordResetService.verifyCodeAndIssueResetToken(
        request.email(),
        request.verificationCode()
    );

    return ResponseEntity.ok(
        ApiResponse.success(
            "인증이 완료되었습니다. 새 비밀번호를 설정해주세요.",
            new PasswordResetVerifyResponse(resetToken)
        )
    );
  }

  /**
   * 3단계: 새 비밀번호 설정 (/api/v1/auth/password/reset/confirm)
   */
  @PostMapping("/reset/confirm")
  @Operation(
      summary = "3단계: 새 비밀번호 설정",
      description = "발급받은 `resetToken`과 새 비밀번호를 사용하여 최종적으로 비밀번호를 변경합니다. 토큰은 사용 즉시 무효화됩니다."
  )
  public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
      @Valid @RequestBody PasswordResetConfirmRequest request
  ) {
    log.info("새 비밀번호 설정 요청 (토큰 사용)");

    passwordResetService.resetPassword(request.resetToken(), request.newPassword());

    return ResponseEntity.ok(
        ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.", null)
    );
  }
}