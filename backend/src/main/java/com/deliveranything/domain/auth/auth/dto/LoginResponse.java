package com.deliveranything.domain.auth.auth.dto;

import com.deliveranything.domain.user.profile.enums.ProfileType;
import java.util.List;
import lombok.Builder;

@Builder
public record LoginResponse(
    Long userId,
    String email,
    String username,
    ProfileType currentActiveProfileType,
    Long currentActiveProfileId,
    List<ProfileType> availableProfiles,
    Long storeId,  // ✅ 판매자 프로필의 상점 ID (없으면 null)
    Object currentProfileDetail
    // ✅ 추가: 현재 활성 프로필 상세 정보 (CustomerProfileDetail | SellerProfileDetail | RiderProfileDetail | null)
) {

}