package com.deliveranything.domain.user.profile.service;

import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileCreateData;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileCreateData;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileCreateData;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.enums.RiderToggleStatus;
import com.deliveranything.domain.user.profile.event.ActiveProfileChangedEvent;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import com.deliveranything.domain.user.profile.repository.ProfileRepository;
import com.deliveranything.domain.user.profile.repository.RiderProfileRepository;
import com.deliveranything.domain.user.profile.repository.SellerProfileRepository;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 프로필 관리 서비스 BaseProfile을 활용한 공통 프로필 작업 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final SellerProfileRepository sellerProfileRepository;
  private final RiderProfileRepository riderProfileRepository;
  private final ApplicationEventPublisher eventPublisher;

  // ========== 프로필 생성 (온보딩과 추가를 통합함)  ==========

  /**
   * 프로필 생성 - 첫 프로필이든 추가 프로필이든 동일하게 처리
   */
  @Transactional
  public Profile createProfile(User user, ProfileType profileType, Object profileData) {

    // 중복 체크
    if (profileRepository.existsByUserIdAndType(user.getId(), profileType)) {
      log.warn("이미 해당 타입의 프로필이 존재합니다: userId={}, profileType={}",
          user.getId(), profileType);
      throw new CustomException(ErrorCode.PROFILE_ALREADY_EXISTS);
    }

    // 1단계: Profile 생성
    Profile profile = Profile.builder()
        .user(user)
        .type(profileType)
        .build();
    profileRepository.save(profile);

    // 2단계: 세부 프로필 생성 (CustomException은 그대로 전파)
    createDetailedProfile(profile, profileType, profileData);

    // 3단계: 첫 프로필이면 자동으로 활성화
    if (!user.hasActiveProfile()) {
      user.setCurrentActiveProfile(profile);
      userRepository.save(user);
      log.info("첫 프로필 생성 및 활성화: userId={}, profileId={}, profileType={}",
          user.getId(), profile.getId(), profileType);
    } else {
      log.info("추가 프로필 생성 완료: userId={}, profileId={}, profileType={}",
          user.getId(), profile.getId(), profileType);
    }

    return profile;
  }

  /**
   * 프로필 전환
   */
  @Transactional
  public boolean switchProfile(User user, ProfileType targetProfile, String deviceId) { // deviceId 추가

    if (!user.hasActiveProfile()) {
      log.warn("프로필이 없는 사용자입니다: userId={}", user.getId());
      throw new CustomException(ErrorCode.PROFILE_REQUIRED);
    }

    // 이전 프로필 ID 저장
    Long oldProfileId = user.getCurrentActiveProfileId();

    Profile targetProfileEntity = profileRepository
        .findByUserIdAndType(user.getId(), targetProfile)
        .orElseThrow(() -> {
          log.warn("해당 프로필을 찾을 수 없습니다: userId={}, targetProfile={}",
              user.getId(), targetProfile);
          return new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        });

    if (user.getCurrentActiveProfileType() == targetProfile) {
      log.info("이미 활성화된 프로필입니다: userId={}, targetProfile={}",
          user.getId(), targetProfile);
      throw new CustomException(ErrorCode.PROFILE_ALREADY_ACTIVE);
    }

    if (!targetProfileEntity.isActive()) {
      log.warn("비활성화된 프로필로는 전환할 수 없습니다: userId={}, targetProfile={}",
          user.getId(), targetProfile);
      throw new CustomException(ErrorCode.PROFILE_INACTIVE);
    }

    try {
      user.switchProfile(targetProfileEntity);
      userRepository.save(user);
      log.info("프로필 전환 완료: userId={}, newActiveProfile={}, profileId={}",
          user.getId(), targetProfile, targetProfileEntity.getId());

      // 이벤트 발행
      eventPublisher.publishEvent(
          new ActiveProfileChangedEvent(oldProfileId, targetProfileEntity.getId(), deviceId));

      return true;
    } catch (IllegalStateException e) {
      log.warn("프로필 전환 실패: userId={}, targetProfile={}, error={}",
          user.getId(), targetProfile, e.getMessage());
      throw new CustomException(ErrorCode.PROFILE_NOT_ALLOWED);
    }
  }

  /**
   * 사용 가능한 프로필 목록 조회
   */
  public List<ProfileType> getAvailableProfiles(User user) {
    return user.getActiveProfileTypes();
  }

  // ========== Profile 관리 헬퍼 메서드 ==========

  /**
   * 프로필 타입별 세부 프로필 생성 CustomException은 그대로 전파하여 호출자가 적절한 에러를 받을 수 있게 함
   */
  private void createDetailedProfile(Profile profile, ProfileType profileType,
      Object profileData) {

    switch (profileType) {
      case CUSTOMER -> {
        if (!(profileData instanceof CustomerProfileCreateData data)) {
          log.error("잘못된 고객 프로필 데이터 타입: {}", profileData.getClass());
          throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        }

        CustomerProfile customerProfile = CustomerProfile.builder()
            .profile(profile)
            .nickname(data.nickname())
            .profileImageUrl(data.profileImageUrl())
            .customerPhoneNumber(data.customerPhoneNumber())
            .build();
        customerProfileRepository.save(customerProfile);
        log.info("고객 프로필 생성 완료: profileId={}", profile.getId());
      }
      case SELLER -> {
        if (!(profileData instanceof SellerProfileCreateData data)) {
          log.error("잘못된 판매자 프로필 데이터 타입: {}", profileData.getClass());
          throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        }

        // 사업자등록번호 중복 체크 - CustomException 그대로 전파
        if (sellerProfileRepository.existsByBusinessCertificateNumber(
            data.businessCertificateNumber())) {
          log.warn("이미 존재하는 사업자등록번호: {}", data.businessCertificateNumber());
          throw new CustomException(ErrorCode.BUSINESS_CERTIFICATE_DUPLICATE);
        }

        SellerProfile sellerProfile = SellerProfile.builder()
            .profile(profile)
            .nickname(data.nickname())
            .profileImageUrl(data.profileImageUrl())
            .businessName(data.businessName())
            .businessCertificateNumber(data.businessCertificateNumber())
            .businessPhoneNumber(data.businessPhoneNumber())
            .bankName(data.bankName())
            .accountNumber(data.accountNumber())
            .accountHolder(data.accountHolder())
            .build();
        sellerProfileRepository.save(sellerProfile);
        log.info("판매자 프로필 생성 완료: profileId={}", profile.getId());
      }
      case RIDER -> {
        if (!(profileData instanceof RiderProfileCreateData data)) {
          log.error("잘못된 배달원 프로필 데이터 타입: {}", profileData.getClass());
          throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
        }

        String area = data.area() != null && !data.area().isBlank() ? data.area() : "서울";

        RiderProfile riderProfile = RiderProfile.builder()
            .profile(profile)
            .nickname(data.nickname())
            .profileImageUrl(data.profileImageUrl())
            .toggleStatus(RiderToggleStatus.OFF)
            .area(area)
            .licenseNumber(data.licenseNumber())
            .bankName("")
            .bankAccountNumber("")
            .bankAccountHolderName("")
            .build();
        riderProfileRepository.save(riderProfile);
        log.info("배달원 프로필 생성 완료: profileId={}", profile.getId());
      }
      default -> {
        log.error("지원하지 않는 프로필 타입: {}", profileType);
        throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
      }
    }
  }

  /**
   * 특정 사용자의 특정 프로필 타입 조회
   */
  public Profile getProfileByUserAndType(User user, ProfileType profileType) {
    return profileRepository.findByUserIdAndType(user.getId(), profileType).orElse(null);
  }

  /**
   * 사용자의 모든 활성 프로필 조회
   */
  public List<Profile> getActiveProfilesByUser(User user) {
    return profileRepository.findActiveProfilesByUser(user);
  }
}