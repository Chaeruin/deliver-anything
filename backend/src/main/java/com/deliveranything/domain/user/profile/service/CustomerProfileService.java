package com.deliveranything.domain.user.profile.service;

import com.deliveranything.domain.user.profile.entity.CustomerAddress;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.repository.CustomerAddressRepository;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import com.deliveranything.domain.user.profile.repository.ProfileRepository;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerProfileService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final CustomerAddressRepository customerAddressRepository;

  // ============================================================
  // 🔹 소비자 프로필 관리
  // ============================================================

  @Transactional
  public CustomerProfile createProfile(Long userId, String nickname) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      log.warn("사용자를 찾을 수 없습니다: userId={}", userId);
      return null;
    }

    if (hasProfile(userId)) {
      log.warn("이미 고객 프로필이 존재합니다: userId={}", userId);
      return getProfile(userId);
    }

    Profile profile = profileRepository.save(Profile.builder()
        .user(user)
        .type(ProfileType.CUSTOMER)
        .build());

    CustomerProfile saved = customerProfileRepository.save(CustomerProfile.builder()
        .profile(profile)
        .nickname(nickname)
        .profileImageUrl(null)
        .build());

    log.info("소비자 프로필 생성 완료: userId={}, profileId={}", userId, saved.getId());
    return saved;
  }

  public CustomerProfile getProfile(Long userId) {
    return customerProfileRepository.findByUserId(userId).orElse(null);
  }

  public CustomerProfile getProfileByProfileId(Long profileId) {
    return customerProfileRepository.findByProfileId(profileId).orElse(null);
  }

  public boolean hasProfile(Long userId) {
    return getProfile(userId) != null;
  }

  @Transactional
  public boolean updateProfileByProfileId(Long profileId, String nickname, String profileImageUrl) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null) {
      log.warn("고객 프로필을 찾을 수 없습니다: profileId={}", profileId);
      return false;
    }

    profile.updateProfile(nickname, profileImageUrl);
    customerProfileRepository.save(profile);

    log.info("고객 프로필 수정 완료: profileId={}, nickname={}", profileId, nickname);
    return true;
  }

  // ============================================================
  // 📦 배송지 관리
  // ============================================================

  public List<CustomerAddress> getAddresses(Long userId) {
    CustomerProfile profile = getProfile(userId);
    if (profile == null) {
      log.warn("고객 프로필을 찾을 수 없습니다: userId={}", userId);
      return List.of();
    }
    return customerAddressRepository.findAddressesByProfile(profile);
  }

  public List<CustomerAddress> getAddressesByProfileId(Long profileId) {
    return customerAddressRepository.findAddressesByProfileId(profileId);
  }

  public CustomerAddress getAddress(Long userId, Long addressId) {
    CustomerProfile profile = getProfile(userId);
    if (profile == null) return null;

    CustomerAddress address = customerAddressRepository.findById(addressId).orElse(null);
    if (address == null || !address.getCustomerProfile().getId().equals(profile.getId())) {
      log.warn("배송지 접근 권한이 없습니다: userId={}, addressId={}", userId, addressId);
      return null;
    }

    return address;
  }

  public CustomerAddress getAddressByProfileId(Long profileId, Long addressId) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null) return null;

    CustomerAddress address = customerAddressRepository.findById(addressId).orElse(null);
    if (address == null || !address.getCustomerProfile().getId().equals(profileId)) {
      log.warn("배송지 접근 권한이 없습니다: profileId={}, addressId={}", profileId, addressId);
      return null;
    }

    return address;
  }

  @Transactional
  public CustomerAddress addAddressByProfileId(Long profileId, String addressName, String address, Double latitude, Double longitude) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null) return null;

    CustomerAddress saved = customerAddressRepository.save(CustomerAddress.builder()
        .customerProfile(profile)
        .addressName(addressName)
        .address(address)
        .latitude(latitude)
        .longitude(longitude)
        .build());

    if (profile.getDefaultAddressId() == null) {
      profile.updateDefaultAddressId(saved.getId());
      customerProfileRepository.save(profile);
    }

    log.info("배송지 추가 완료: profileId={}, addressId={}", profileId, saved.getId());
    return saved;
  }

  @Transactional
  public boolean updateAddress(Long userId, Long addressId, String addressName, String address, Double latitude, Double longitude) {
    CustomerAddress customerAddress = getAddress(userId, addressId);
    if (customerAddress == null) return false;

    customerAddress.updateAddress(addressName, address, latitude, longitude);
    customerAddressRepository.save(customerAddress);

    log.info("배송지 수정 완료: userId={}, addressId={}", userId, addressId);
    return true;
  }

  @Transactional
  public boolean deleteAddress(Long userId, Long addressId) {
    CustomerAddress customerAddress = getAddress(userId, addressId);
    if (customerAddress == null || customerAddress.isDefault()) return false;

    customerAddressRepository.delete(customerAddress);
    log.info("배송지 삭제 완료: userId={}, addressId={}", userId, addressId);
    return true;
  }

  @Transactional
  public boolean setDefaultAddress(Long userId, Long addressId) {
    CustomerProfile profile = getProfile(userId);
    CustomerAddress address = getAddress(userId, addressId);
    if (profile == null || address == null) return false;

    profile.updateDefaultAddressId(addressId);
    customerProfileRepository.save(profile);

    log.info("기본 배송지 설정 완료: userId={}, profileId={}, addressId={}", userId, profile.getId(), addressId);
    return true;
  }

  public CustomerAddress getCurrentAddress(Long userId) {
    CustomerProfile profile = getProfile(userId);
    if (profile == null || profile.getDefaultAddressId() == null) return null;
    return customerAddressRepository.findById(profile.getDefaultAddressId()).orElse(null);
  }

  public CustomerAddress getCurrentAddressByProfileId(Long profileId) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null || profile.getDefaultAddressId() == null) return null;
    return customerAddressRepository.findById(profile.getDefaultAddressId()).orElse(null);
  }
}