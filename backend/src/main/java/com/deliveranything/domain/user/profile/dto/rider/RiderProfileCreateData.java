package com.deliveranything.domain.user.profile.dto.rider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 배달원 프로필 생성 데이터
 */
public record RiderProfileCreateData(
    @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요.")
    String nickname,

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하로 입력해주세요.")
    String profileImageUrl,

    @NotBlank(message = "면허번호는 필수 입력 사항입니다.")
    @Size(max = 50, message = "면허번호는 50자 이하로 입력해주세요.")
    String licenseNumber,

    @Size(max = 100, message = "활동 지역은 100자 이하로 입력해주세요.")
    String area

    // 은행 정보 (선택사항)

) {

}