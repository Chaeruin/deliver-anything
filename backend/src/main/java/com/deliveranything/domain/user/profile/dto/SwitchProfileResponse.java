package com.deliveranything.domain.user.profile.dto;

import com.deliveranything.domain.user.profile.enums.ProfileType;
import lombok.Builder;

/**
 * 프로필 전환 응답 DTO
 */
@Builder
public record SwitchProfileResponse(
    Long userId,
    ProfileType previousProfileType,
    Long previousProfileId,
    ProfileType currentProfileType,
    Long currentProfileId,
    Long storeId,            // 판매자 프로필의 상점 ID (없으면 null)
    Object currentProfileDetail  // 추가: 현재 활성 프로필 상세 정보

) {

  // API 응답용 변환 - 토큰 제거
  public SwitchProfileResponse toResponse() {
    return SwitchProfileResponse.builder()
        .userId(userId)
        .previousProfileType(previousProfileType)
        .previousProfileId(previousProfileId)
        .currentProfileType(currentProfileType)
        .currentProfileId(currentProfileId)
        .storeId(storeId)
        .currentProfileDetail(currentProfileDetail)
        .build();
  }
}