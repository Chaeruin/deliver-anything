package com.deliveranything.domain.auth.verification.dto;

import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerificationVerifyRequest(
    @NotBlank(message = "식별자는 필수 입력 사항입니다.")
    String identifier,

    @NotBlank(message = "인증 코드는 필수 입력 사항입니다.")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리 숫자입니다.")
    String verificationCode,

    @NotNull(message = "인증 타입은 필수 입력 사항입니다.")
    VerificationType verificationType,

    @NotNull(message = "인증 목적은 필수 입력 사항입니다.")
    VerificationPurpose purpose
) {

}