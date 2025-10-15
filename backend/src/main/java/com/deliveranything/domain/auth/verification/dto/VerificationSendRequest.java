package com.deliveranything.domain.auth.verification.dto;

import com.deliveranything.domain.auth.verification.enums.VerificationPurpose;
import com.deliveranything.domain.auth.verification.enums.VerificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerificationSendRequest(
    @NotBlank(message = "식별자는 필수 입력 사항입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String identifier,

    @NotNull(message = "인증 타입은 필수 입력 사항입니다.")
    VerificationType verificationType,

    @NotNull(message = "인증 목적은 필수 입력 사항입니다.")
    VerificationPurpose purpose
) {

}