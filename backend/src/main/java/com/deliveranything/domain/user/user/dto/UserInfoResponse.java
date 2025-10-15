package com.deliveranything.domain.user.user.dto;

import com.deliveranything.domain.auth.auth.enums.SocialProvider;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.user.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UserInfoResponse(
    Long userId,
    String email,
    String username,
    String phoneNumber,
    SocialProvider socialProvider,
    ProfileType currentActiveProfileType,
    Long currentActiveProfileId,
    boolean isEmailVerified,
    boolean isAdmin,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt
) {

  /**
   * User 엔티티로부터 UserInfoResponse 생성 (정적 팩토리 메서드)
   */
  public static UserInfoResponse from(User user) {
    return UserInfoResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .phoneNumber(user.getPhoneNumber())
        .socialProvider(user.getSocialProvider())
        .currentActiveProfileType(user.getCurrentActiveProfileType())
        .currentActiveProfileId(user.getCurrentActiveProfileId())
        .isEmailVerified(user.isEmailVerified())
        .isAdmin(user.isAdmin())
        .createdAt(user.getCreatedAt())
        .lastLoginAt(user.getLastLoginAt())
        .build();
  }
}