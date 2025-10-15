package com.deliveranything.domain.user.profile.service;

import com.deliveranything.domain.user.profile.entity.CustomerAddress;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.repository.CustomerAddressRepository;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerProfileService {

  private final CustomerProfileRepository customerProfileRepository;
  private final CustomerAddressRepository customerAddressRepository;

  // ========== 레거시 호환 메서드 (다른 도메인에서 사용) ==========

  public CustomerProfile getProfileByUserId(Long userId) {
    return customerProfileRepository.findByUserId(userId).orElse(null);
  }

  // ========== 실제 사용 메서드 ==========

  public CustomerProfile getProfileByProfileId(Long profileId) {
    return customerProfileRepository.findByProfileId(profileId).orElse(null);
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

  public List<CustomerAddress> getAddressesByProfileId(Long profileId) {
    return customerAddressRepository.findAddressesByProfileId(profileId);
  }

  public CustomerAddress getAddressByProfileId(Long profileId, Long addressId) {
    CustomerAddress address = customerAddressRepository.findById(addressId).orElse(null);
    if (address == null || !address.getCustomerProfile().getId().equals(profileId)) {
      log.warn("배송지 접근 권한이 없습니다: profileId={}, addressId={}", profileId, addressId);
      return null;
    }
    return address;
  }

  @Transactional
  public CustomerAddress addAddressByProfileId(Long profileId, String addressName, String address,
      Double latitude, Double longitude) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null) {
      return null;
    }

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
  public boolean updateAddress(Long profileId, Long addressId, String addressName, String address,
      Double latitude, Double longitude) {
    CustomerAddress customerAddress = customerAddressRepository.findById(addressId).orElse(null);
    if (customerAddress == null) {
      return false;
    }

    customerAddress.updateAddress(addressName, address, latitude, longitude);
    customerAddressRepository.save(customerAddress);

    log.info("배송지 수정 완료: profileId={}, addressId={}", profileId, addressId);
    return true;
  }

  @Transactional
  public boolean deleteAddress(Long userId, Long addressId) {
    CustomerAddress customerAddress = customerAddressRepository.findById(addressId).orElse(null);
    if (customerAddress == null || customerAddress.isDefault()) {
      return false;
    }

    customerAddressRepository.delete(customerAddress);
    log.info("배송지 삭제 완료: userId={}, addressId={}", userId, addressId);
    return true;
  }

  @Transactional
  public boolean setDefaultAddress(Long profileId, Long addressId) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    CustomerAddress address = customerAddressRepository.findById(addressId).orElse(null);
    if (profile == null || address == null) {
      return false;
    }

    profile.updateDefaultAddressId(addressId);
    customerProfileRepository.save(profile);

    log.info("기본 배송지 설정 완료: profileId={}, addressId={}", profileId, addressId);
    return true;
  }

  public CustomerAddress getAddress(Long addressId, long l) {
    return customerAddressRepository.findById(addressId).orElse(null);
  }
  
  public CustomerAddress getCurrentAddress(Long profileId) {
    CustomerProfile profile = getProfileByProfileId(profileId);
    if (profile == null || profile.getDefaultAddressId() == null) {
      return null;
    }
    return customerAddressRepository.findById(profile.getDefaultAddressId()).orElse(null);
  }

}