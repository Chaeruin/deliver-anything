package com.deliveranything.domain.auth.service;

import com.deliveranything.domain.auth.enums.SocialProvider;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.domain.user.profile.dto.SwitchProfileResponse;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileDetail;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileDetail;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileDetail;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import com.deliveranything.domain.user.profile.repository.ProfileRepository;
import com.deliveranything.domain.user.profile.repository.RiderProfileRepository;
import com.deliveranything.domain.user.profile.repository.SellerProfileRepository;
import com.deliveranything.domain.user.profile.service.ProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  private final TokenService tokenService;
  private final ProfileService profileService;
  private final TokenBlacklistService tokenBlacklistService;
  private final StoreService storeService;

  private final PasswordEncoder passwordEncoder;

  // 프로필 상세 조회용 Repository 추가
  private final CustomerProfileRepository customerProfileRepository;
  private final SellerProfileRepository sellerProfileRepository;
  private final RiderProfileRepository riderProfileRepository;


  /**
   * 일반 회원가입
   */
  @Transactional
  public User signup(String email, String password, String username, String phoneNumber) {
    return signup(email, password, username, phoneNumber, SocialProvider.LOCAL, null);
  }

  /**
   * OAuth2 회원가입 (내부용)
   */
  @Transactional
  public User signupOAuth2(String email, String username, SocialProvider socialProvider,
      String socialId) {
    return signup(email, null, username, null, socialProvider, socialId);
  }

  /**
   * 통합 회원가입 로직 (멀티 프로필 지원)
   */
  private User signup(
      String email,
      String password,
      String username,
      String phoneNumber,
      SocialProvider socialProvider,
      String socialId
  ) {
    // 중복 체크
    if (email != null && userRepository.existsByEmail(email)) {
      log.warn("이미 존재하는 이메일: {}", email);
      throw new CustomException(ErrorCode.USER_EMAIL_ALREADY_EXIST);
    }

    if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
      log.warn("이미 존재하는 전화번호: {}", phoneNumber);
      throw new CustomException(ErrorCode.USER_PHONE_ALREADY_EXIST);
    }

    // 비밀번호 암호화
    String encodedPassword = (password != null && !password.isBlank())
        ? passwordEncoder.encode(password)
        : "";

    User newUser = User.builder()
        .email(email)
        .password(encodedPassword)
        .username(username)
        .phoneNumber(phoneNumber)
        .socialProvider(socialProvider)
        .socialId(socialId)
        .build();

    // OAuth2는 이메일 인증 자동 완료
    if (socialProvider != SocialProvider.LOCAL) {
      newUser.verifyEmail();
    }

    User savedUser = userRepository.save(newUser);
    log.info("신규 사용자 가입 완료: userId={}, email={}, provider={}",
        savedUser.getId(), email, socialProvider);

    return savedUser;
  }

  /**
   * 로그인 (storeId + 프로필 상세 정보 포함)
   */
  @Transactional
  public LoginResult login(String email, String password, String deviceInfo) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (!user.isEnabled()) {
      log.warn("비활성화된 계정 로그인 시도: email={}", email);
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    // 비밀번호 검증
    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("잘못된 비밀번호 로그인 시도: email={}", email);
      throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    // 마지막 로그인 시간 업데이트
    user.updateLastLoginAt();
    userRepository.save(user);

    log.info("로그인 성공: userId={}, email={}", user.getId(), email);

    // 토큰 발급
    String accessToken = tokenService.genAccessToken(user);
    String refreshToken = tokenService.genRefreshToken(user, deviceInfo);

    // 추가: storeId 조회
    Long storeId = getStoreIdIfSeller(user);

    // 추가: 프로필 상세 정보 조회
    Object profileDetail = getCurrentProfileDetail(user);

    return new LoginResult(user, accessToken, refreshToken, storeId, profileDetail);
  }

  /**
   * OAuth2 로그인 또는 회원가입
   */
  @Transactional
  public User oAuth2SignupOrLogin(
      String email,
      String username,
      SocialProvider socialProvider,
      String socialId
  ) {
    // 1. socialId로 기존 사용자 찾기
    User user = userRepository.findBySocialProviderAndSocialId(socialProvider, socialId)
        .orElse(null);

    // 2. 기존 사용자면 로그인 + 소셜 정보 업데이트
    if (user != null) {
      log.info("기존 OAuth2 사용자 로그인: userId={}, provider={}", user.getId(), socialProvider);
      user.updateSocialInfo(username, email);
      user.updateLastLoginAt();
      return userRepository.save(user);
    }

    // 3. 신규 사용자 - signupOAuth2를 통한 회원가입
    log.info("신규 OAuth2 사용자 가입: email={}, provider={}", email, socialProvider);
    return signupOAuth2(email, username, socialProvider, socialId);
  }

  /**
   * 로그아웃 (현재 기기만)
   */
  @Transactional
  public void logout(Long userId, String deviceInfo, String accessToken) {
    // 특정 기기의 Refresh Token 무효화
    tokenService.invalidateRefreshToken(userId, deviceInfo);
    log.info("로그아웃 완료: userId={}, deviceInfo={}", userId, deviceInfo);

    // Access Token 블랙리스트 등록
    if (accessToken != null && !accessToken.isEmpty()) {
      tokenBlacklistService.addToBlacklist(accessToken);
      log.info("로그아웃 완료 및 accessToken 블랙리스트 등록: userId={}", userId);
    } else {
      log.info("로그아웃 완료: userId={}, deviceInfo={}", userId, deviceInfo);
    }

  }

  /**
   * 전체 로그아웃 (모든 기기)
   */
  @Transactional
  public void logoutAll(Long userId, String accessToken) {
    // 모든 기기 리프레시 토큰 무효화
    tokenService.invalidateAllRefreshTokens(userId);
    log.info("전체 로그아웃 완료: userId={}", userId);

    // 현재 Access Token 블랙리스트 등록 -> 다른 기기들의 Access Token은 자연 만료(직접 추적해서 모두 수동만료 시키려면 JWT의 stateless 장점 사라진다고 생각해서 이렇게 구현 )
    if (accessToken != null && !accessToken.isEmpty()) {
      tokenBlacklistService.addToBlacklist(accessToken);
      log.info("전체 로그아웃 완료 및 accessToken 블랙리스트 등록: userId={}", userId);
    } else {
      log.info("전체 로그아웃 완료: userId={}", userId);
    }
  }

  /**
   * 이메일 인증
   */
  @Transactional
  public void verifyEmail(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    user.verifyEmail();
    userRepository.save(user);

    log.info("이메일 인증 완료: userId={}", userId);
  }

  /**
   * 프로필 전환 + Access Token 재발급 (Refresh Token 유지)
   */
  @Transactional
  public SwitchProfileResponse switchProfileWithTokenReissue(
      Long userId,
      ProfileType targetProfileType,
      String oldAccessToken) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // 온보딩 체크 제거 → 프로필 존재 여부로 변경
    if (!user.hasActiveProfile()) {
      log.warn("프로필이 없는 사용자입니다: userId={}", userId);
      throw new CustomException(ErrorCode.PROFILE_REQUIRED);
    }

    // 현재 프로필 정보 저장
    ProfileType previousProfileType = user.getCurrentActiveProfileType();
    Long previousProfileId = user.getCurrentActiveProfileId();

    // 타겟 프로필 조회
    Profile targetProfile = profileRepository
        .findByUserIdAndType(userId, targetProfileType)
        .orElseThrow(() -> {
          log.warn("해당 프로필을 찾을 수 없습니다: userId={}, targetProfile={}",
              userId, targetProfileType);
          return new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        });

    // 이미 활성화된 프로필인 경우 - Access Token만 재발급
    if (user.getCurrentActiveProfileType() == targetProfileType) {
      log.info("이미 활성화된 프로필입니다: userId={}, targetProfile={}",
          userId, targetProfileType);

      String newAccessToken = tokenService.genAccessToken(user);

      // 기존 AccessToken 블랙리스트 등록
      if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
        tokenBlacklistService.addToBlacklist(oldAccessToken);
        log.info("프로필 전환 - 기존 accessToken 블랙리스트 등록: userId={}", userId);
      }

      // storeId 조회
      Long storeId = getStoreIdIfSeller(user);

      // 프로필 상세 정보 조회
      Object profileDetail = getCurrentProfileDetail(user);

      return SwitchProfileResponse.builder()
          .userId(userId)
          .previousProfileType(previousProfileType)
          .previousProfileId(previousProfileId)
          .currentProfileType(targetProfileType)
          .currentProfileId(targetProfile.getId())
          .storeId(storeId)
          .currentProfileDetail(profileDetail)
          .accessToken(newAccessToken)
          .build();
    }

    // 프로필 전환 수행 (User 객체로 전달)
    boolean switched = profileService.switchProfile(user, targetProfileType);

    if (!switched) {
      log.error("프로필 전환 실패: userId={}, targetProfile={}", userId, targetProfileType);
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    // 전환된 user로 새 Access Token 생성
    User updatedUser = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    // Access Token만 재발급 (Refresh Token 유지)
    String newAccessToken = tokenService.genAccessToken(updatedUser);

    // 기존 AccessToken 블랙리스트 등록
    if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
      tokenBlacklistService.addToBlacklist(oldAccessToken);
      log.info("프로필 전환 완료 및 기존 accessToken 블랙리스트 등록: userId={}", userId);
    }

    // storeId 조회
    Long storeId = getStoreIdIfSeller(updatedUser);

    // 프로필 상세 정보 조회
    Object profileDetail = getCurrentProfileDetail(updatedUser);

    log.info("프로필 전환 완료 및 Access Token 재발급: userId={}, {} -> {}",
        userId, previousProfileType, targetProfileType);

    return SwitchProfileResponse.builder()
        .userId(userId)
        .previousProfileType(previousProfileType)
        .previousProfileId(previousProfileId)
        .currentProfileType(targetProfileType)
        .currentProfileId(targetProfile.getId())
        .storeId(storeId)
        .currentProfileDetail(profileDetail)
        .accessToken(newAccessToken)
        .build();
  }

  /**
   * 판매자 프로필인 경우 상점 ID 조회 판매자가 아니거나 상점이 없으면 null 반환
   */
  private Long getStoreIdIfSeller(User user) {
    // 판매자 프로필이 아니면 null
    if (user.getCurrentActiveProfileType() != ProfileType.SELLER) {
      return null;
    }

    Long sellerProfileId = user.getCurrentActiveProfileId();
    if (sellerProfileId == null) {
      return null;
    }

    return storeService.getStoreIdBySellerProfileId(sellerProfileId);
  }

  /**
   * 현재 활성 프로필의 상세 정보 조회 프로필 없으면 null 반환
   */
  private Object getCurrentProfileDetail(User user) {
    // 온보딩 체크 제거 → 프로필 존재 여부로 변경
    if (!user.hasActiveProfile()) {
      return null;
    }

    Long profileId = user.getCurrentActiveProfileId();
    ProfileType profileType = user.getCurrentActiveProfileType();

    if (profileId == null || profileType == null) {
      return null;
    }

    return switch (profileType) {
      case CUSTOMER -> {
        CustomerProfile profile = customerProfileRepository.findById(profileId)
            .orElse(null);
        yield CustomerProfileDetail.from(profile);
      }
      case SELLER -> {
        SellerProfile profile = sellerProfileRepository.findById(profileId)
            .orElse(null);
        yield SellerProfileDetail.from(profile);
      }
      case RIDER -> {
        RiderProfile profile = riderProfileRepository.findById(profileId)
            .orElse(null);
        yield RiderProfileDetail.from(profile);
      }
    };
  }

  // 내부 DTO - storeId, profileDetail 추가
  public record LoginResult(
      User user,
      String accessToken,
      String refreshToken,
      Long storeId,  // 상점 ID
      Object currentProfileDetail  // 프로필 상세 정보
  ) {

  }
}