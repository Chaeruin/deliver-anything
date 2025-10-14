package com.deliveranything.domain.user.profile.dto.rider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    String area,

    // 은행 정보 (선택사항)
    @Size(max = 50, message = "은행명은 50자 이하로 입력해주세요.")
    String bankName,

    @Pattern(regexp = "^\\d{10,14}$", message = "계좌번호는 10~14자리 숫자여야 합니다.")
    String accountNumber,

    @Size(max = 50, message = "예금주는 50자 이하로 입력해주세요.")
    String accountHolder,

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Size(max = 20, message = "전화번호는 20자 이하로 입력해주세요.")
    String riderPhoneNumber
) {

}