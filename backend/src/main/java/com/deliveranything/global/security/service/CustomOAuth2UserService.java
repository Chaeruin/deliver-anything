package com.deliveranything.global.security.service;

import com.deliveranything.domain.auth.auth.enums.SocialProvider;
import com.deliveranything.domain.auth.auth.service.AuthService;
import com.deliveranything.domain.auth.auth.service.UserAuthorityProvider;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.global.security.auth.SecurityUser;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final AuthService authService;
  private final UserAuthorityProvider userAuthorityProvider;

  /**
   * OAuth2 로그인 성공 시 호출
   */
  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 1. OAuth2 기본 정보 로드
    OAuth2User oAuth2User = super.loadUser(userRequest);

    // 2. 제공자 정보 추출
    String providerTypeCode = userRequest.getClientRegistration()
        .getRegistrationId()
        .toUpperCase();

    // 3. 제공자별 사용자 정보 추출
    String oauthUserId;
    String email = null;
    String name;

    switch (providerTypeCode) {
      case "KAKAO" -> {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        oauthUserId = oAuth2User.getName();
        name = (String) properties.get("nickname");

        // 카카오는 이메일 제공 동의 필요 (없을 수 있음)
        if (kakaoAccount != null) {
          email = (String) kakaoAccount.get("email");
        }
      }
      case "GOOGLE" -> {
        oauthUserId = oAuth2User.getName();
        name = (String) oAuth2User.getAttributes().get("name");
        email = (String) oAuth2User.getAttributes().get("email");
      }
      case "NAVER" -> {
        Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttributes()
            .get("response");

        oauthUserId = (String) response.get("id");
        name = (String) response.get("nickname");
        email = (String) response.get("email");
      }
      default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + providerTypeCode);
    }

    // 4. SocialProvider enums 변환
    SocialProvider socialProvider = SocialProvider.valueOf(providerTypeCode);

    // 5. 회원가입 또는 로그인 처리 (프로필 생성 없이 User만)
    User user = authService.oAuth2SignupOrLogin(email, name, socialProvider, oauthUserId);

    log.info("OAuth2 인증 완료: userId={}, provider={}",
        user.getId(), socialProvider);

    // 6. SecurityUser 생성 (currentActiveProfile이 null일 수 있음)
    return new SecurityUser(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getPassword(),
        user.getCurrentActiveProfile(), // 프로필 생성 전이면 null
        userAuthorityProvider.getAuthorities(user)
    );
  }
}