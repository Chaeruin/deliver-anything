package com.deliveranything.global.security.auth;

import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class SecurityUser extends User implements OAuth2User {

  private final Long id;
  private final String name;
  private final String email;
  private final Profile currentActiveProfile;  // 이제 전역 고유 Profile ID

  public SecurityUser(
      Long id,
      String name,
      String password,
      String email,
      Profile currentActiveProfile,
      Collection<? extends GrantedAuthority> authorities
  ) {
    super(String.valueOf(id), password != null ? password : "", authorities);
    this.id = id;
    this.name = name;
    this.email = email;
    this.currentActiveProfile = currentActiveProfile;
  }

  // OAuth2User 인터페이스 구현
  @Override
  public Map<String, Object> getAttributes() {
    return Map.of();  // ✅ 빈 맵 반환
  }

  @Override  // OAuth2User 인터페이스의 getName 메서드 구현
  public String getName() {
    return String.valueOf(id);
  }

  // Rq에서 쓰일 일반 유저네임을 위한 게터 추가
  public String getRealName() {
    return this.name;
  }

  // 현재 활성화된 프로필이 있는지 확인
  public boolean hasActiveProfile() {
    return currentActiveProfile != null;
  }

  // 현재 활성화된 프로필이 특정 타입인지 확인
  public boolean hasActiveProfile(ProfileType profileType) {
    // null 체크 추가
    return currentActiveProfile != null && currentActiveProfile.getType() == profileType;
  }

  // 현재 활성화된 프로필 ID 반환 (null 안전)
  public Long getCurrentActiveProfileIdSafe() {
    return currentActiveProfile != null ? currentActiveProfile.getId() : null;
  }

  // 고객 프로필 활성화 여부
  public boolean isCustomerActive() {
    return hasActiveProfile(ProfileType.CUSTOMER);
  }

  // 판매자 프로필 활성화 여부
  public boolean isSellerActive() {
    return hasActiveProfile(ProfileType.SELLER);
  }

  // 라이더 프로필 활성화 여부
  public boolean isRiderActive() {
    return hasActiveProfile(ProfileType.RIDER);
  }
}