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

    String profileImageUrl,  // 선택사항

    @NotBlank(message = "면허번호는 필수 입력 사항입니다.")
    @Size(max = 50, message = "면허번호는 50자 이하로 입력해주세요.")
    String licenseNumber,

    @Size(max = 100, message = "활동 지역은 100자 이하로 입력해주세요.")
    String area,  // 선택사항, 기본값: "서울"

    // 은행 정보 추가 (선택사항)
    @Size(max = 50, message = "은행명은 50자 이하로 입력해주세요.")
    String bankName,

    @Pattern(regexp = "^\\d{10,14}$", message = "계좌번호는 10~14자리 숫자여야 합니다.")
    String accountNumber,

    @Size(max = 50, message = "예금주는 50자 이하로 입력해주세요.")
    String accountHolder
) {

}