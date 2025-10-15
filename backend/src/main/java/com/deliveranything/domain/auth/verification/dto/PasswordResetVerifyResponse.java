package com.deliveranything.domain.auth.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비밀번호 재설정 2단계 성공 시 클라이언트에게 반환되는 DTO
 */
public record PasswordResetVerifyResponse(
    @Schema(description = "비밀번호 재설정 3단계(`/confirm`)에서 사용될 일회용 토큰",
        example = "a1b2c3d4-e5f6-7890-abcd-ef0123456789")
    String resetToken
) {

}
