package com.deliveranything.domain.auth.auth.service;

import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.user.entity.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAuthorityProvider {

  /**
   * User 엔티티의 상태를 기반으로 권한 목록 생성
   *
   * @param user 권한을 생성할 사용자
   * @return Spring Security GrantedAuthority 목록
   */
  public Collection<? extends GrantedAuthority> getAuthorities(User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // 1. 기본 권한 (모든 인증된 사용자)
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // 2. 관리자 권한
    if (user.isAdmin()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      log.debug("관리자 권한 부여: userId={}", user.getId());
    }

    // 3. 현재 활성 프로필 기반 권한
    Profile currentActiveProfile = user.getCurrentActiveProfile();
    if (currentActiveProfile != null && currentActiveProfile.isActive()) {
      ProfileType activeType = currentActiveProfile.getType();
      authorities.add(new SimpleGrantedAuthority("ROLE_" + activeType.name()));
      log.debug("프로필 권한 부여: userId={}, profileType={}, profileId={}",
          user.getId(), activeType, currentActiveProfile.getId());
    }

    // 4. (선택사항) 보유한 모든 활성 프로필 권한 추가
    // 현재는 "현재 활성 프로필"만 권한으로 부여하는 단일 프로필 모드
    // 혹시나 나중에 멀티 프로필 권한이 필요하다면 아래 주석 해제
    /*
    user.getProfiles().stream()
        .filter(Profile::isActive)
        .map(profile -> new SimpleGrantedAuthority("PROFILE_" + profile.getType().name()))
        .forEach(authorities::add);
    */

    return authorities;
  }
}