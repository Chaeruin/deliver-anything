package com.deliveranything.domain.user.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank(message = "이름은 필수 입력 사항입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
    String username,

    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Size(max = 20, message = "전화번호는 20자 이하로 입력해주세요.")
    String phoneNumber
) {

}