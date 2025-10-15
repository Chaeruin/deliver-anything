package com.deliveranything.domain.auth.verification.dto;

import jakarta.validation.constraints.NotBlank;

public record MyEmailVerifyRequest(
    @NotBlank(message = "인증 코드는 필수 입력 사항입니다.")
    String verificationCode // 클라이언트가 보낼 유일한 필수 필드
) {

}