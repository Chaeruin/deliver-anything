package com.deliveranything.domain.user.profile.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 소비자 프로필 생성 데이터
 */
public record CustomerProfileCreateData(
    @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    String nickname,

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하로 입력해주세요.")
    String profileImageUrl,

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678 또는 01012345678)")
    // 하이픈 선택적 허용, 010, 011, 016, 017, 018, 019로 시작
    @Size(max = 20, message = "전화번호는 20자 이하로 입력해주세요.")
    String customerPhoneNumber
) {

}