package com.deliveranything.domain.auth.verification.controller;

import com.deliveranything.domain.auth.verification.dto.VerificationSendRequest;
import com.deliveranything.domain.auth.verification.dto.VerificationVerifyRequest;
import com.deliveranything.domain.auth.verification.service.VerificationService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.SecurityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입, 일반적인 비밀번호 찾기 1단계 등 로그인 없이 접근 가능한 인증/검증 API를 제공합니다. 기본 경로: /api/v1/auth/verification
 */
@Tag(name = "일반 이메일 / SMS 인증/검증 API", description = "로그인 없이 사용 가능한 인증 코드 발송 및 검증 (회원가입, 비밀번호 찾기 1단계 등) - SMS는 추후 구현 예정")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/verification")
@RequiredArgsConstructor
public class VerificationController {

  private final VerificationService verificationService;
  private final Rq rq;
  private final SecurityService securityService;

  /**
   * 인증 코드 발송 (/api/v1/auth/verification/send)
   */
  @PostMapping("/send")
  @Operation(
      summary = "인증 코드 발송",
      description = "이메일 또는 SMS로 인증 코드를 발송합니다. (현재는 이메일만 지원). 요청 시 `purpose`를 명시해야 합니다."
  )
  public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
      @Valid @RequestBody VerificationSendRequest request
  ) {
    log.info("인증 코드 발송 요청: identifier={}, type={}, purpose={}",
        request.identifier(), request.verificationType(), request.purpose());

    verificationService.sendVerificationCode(request);

    return ResponseEntity.ok(
        ApiResponse.success("인증 코드가 발송되었습니다.", null)
    );
  }

  /**
   * 인증 코드 검증 (/api/v1/auth/verification/verify)
   */
  @PostMapping("/verify")
  @Operation(
      summary = "인증 코드 검증",
      description = "사용자가 입력한 인증 코드를 검증합니다. 성공 시 해당 인증 정보는 Redis에서 삭제됩니다."
  )
  public ResponseEntity<ApiResponse<Void>> verifyCode(
      @Valid @RequestBody VerificationVerifyRequest request
  ) {
    log.info("인증 코드 검증 요청: identifier={}, type={}, purpose={}",
        request.identifier(), request.verificationType(), request.purpose());

    verificationService.verifyCode(request);

    return ResponseEntity.ok(
        ApiResponse.success("인증이 완료되었습니다.", null)
    );
  }
}