package com.deliveranything.domain.user.profile.dto;

import com.deliveranything.domain.user.profile.enums.ProfileType;
import lombok.Builder;

/**
 * 프로필 생성 응답
 */
@Builder
public record CreateProfileResponse(
    Long userId,
    ProfileType profileType,
    Long profileId,
    String nickname,
    boolean isActive,
    String message
) {

}