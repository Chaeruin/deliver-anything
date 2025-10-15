package com.deliveranything.domain.user.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.entity.CustomerAddress;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.repository.CustomerAddressRepository;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import com.deliveranything.domain.user.profile.repository.ProfileRepository;
import com.deliveranything.domain.user.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerProfileService 단위 테스트")
class CustomerProfileServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private CustomerProfileRepository customerProfileRepository;

  @Mock
  private CustomerAddressRepository customerAddressRepository;

  @InjectMocks
  private CustomerProfileService customerProfileService;

  // ❌ 삭제: createProfile 테스트 (메서드가 서비스에 없음)
  // @Nested
  // @DisplayName("프로필 생성 테스트")
  // class CreateProfileTest { ... }

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - userId로 프로필 조회")
    void getProfile_success() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(customerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

      CustomerProfile result = customerProfileService.getProfileByUserId(1L);

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void getProfile_not_found() {
      when(customerProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

      CustomerProfile result = customerProfileService.getProfileByUserId(1L);

      assertNull(result);
    }

    @Test
    @DisplayName("성공 - profileId로 프로필 조회")
    void getProfileByProfileId_success() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerProfile result = customerProfileService.getProfileByProfileId(10L);

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }

    // ❌ 삭제: hasProfile 테스트 (메서드가 서비스에 없음)
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 프로필 수정")
    void updateProfileByProfileId_success() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(customerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = customerProfileService.updateProfileByProfileId(
          10L, "새닉네임", "http://new-image.url"
      );

      assertTrue(result);
      verify(mockProfile, times(1)).updateProfile("새닉네임", "http://new-image.url");
      verify(customerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateProfileByProfileId_fail_not_found() {
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      boolean result = customerProfileService.updateProfileByProfileId(
          10L, "새닉네임", "http://new-image.url"
      );

      assertFalse(result);
      verify(customerProfileRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("배송지 목록 조회 테스트")
  class GetAddressesTest {

    // ❌ 삭제: getAddresses(userId) 테스트 (메서드가 서비스에 없음)

    @Test
    @DisplayName("성공 - profileId로 배송지 목록 조회")
    void getAddressesByProfileId_success() {
      CustomerAddress address1 = mock(CustomerAddress.class);
      CustomerAddress address2 = mock(CustomerAddress.class);
      when(customerAddressRepository.findAddressesByProfileId(10L))
          .thenReturn(List.of(address1, address2));

      List<CustomerAddress> result = customerProfileService.getAddressesByProfileId(10L);

      assertNotNull(result);
      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("성공 - 배송지 없음")
    void getAddressesByProfileId_empty() {
      when(customerAddressRepository.findAddressesByProfileId(10L))
          .thenReturn(List.of());

      List<CustomerAddress> result = customerProfileService.getAddressesByProfileId(10L);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("배송지 단건 조회 테스트")
  class GetAddressTest {

    @Test
    @DisplayName("성공 - profileId로 배송지 조회")
    void getAddressByProfileId_success() {

      CustomerAddress mockAddress = mock(CustomerAddress.class);
      CustomerProfile customerProfile = mock(CustomerProfile.class);
      when(customerProfile.getId()).thenReturn(10L);
      when(mockAddress.getCustomerProfile()).thenReturn(customerProfile);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));

      CustomerAddress result = customerProfileService.getAddressByProfileId(10L, 1L);

      assertNotNull(result);
      assertEquals(mockAddress, result);
    }

    @Test
    @DisplayName("실패 - 배송지 없음")
    void getAddressByProfileId_not_found() {
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.empty());

      CustomerAddress result = customerProfileService.getAddressByProfileId(10L, 1L);

      assertNull(result);
    }

    @Test
    @DisplayName("실패 - 소유자 불일치")
    void getAddressByProfileId_not_owned() {
      CustomerAddress mockAddress = mock(CustomerAddress.class);
      CustomerProfile addressProfile = mock(CustomerProfile.class);
      when(addressProfile.getId()).thenReturn(20L);  // 다른 프로필 ID
      when(mockAddress.getCustomerProfile()).thenReturn(addressProfile);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));

      CustomerAddress result = customerProfileService.getAddressByProfileId(10L, 1L);

      assertNull(result);
    }
  }

  @Nested
  @DisplayName("배송지 추가 테스트")
  class AddAddressTest {

    @Test
    @DisplayName("성공 - 배송지 추가 (첫 배송지)")
    void addAddressByProfileId_success_first() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(mockProfile.getDefaultAddressId()).thenReturn(null);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(mockAddress.getId()).thenReturn(1L);
      when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(mockAddress);
      when(customerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      CustomerAddress result = customerProfileService.addAddressByProfileId(
          10L, "집", "서울시 강남구", 37.123, 127.123
      );

      assertNotNull(result);
      verify(mockProfile, times(1)).updateDefaultAddressId(1L);
      verify(customerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("성공 - 배송지 추가 (기본 배송지 이미 존재)")
    void addAddressByProfileId_success_not_first() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(mockProfile.getDefaultAddressId()).thenReturn(1L);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(mockAddress);

      CustomerAddress result = customerProfileService.addAddressByProfileId(
          10L, "회사", "서울시 서초구", 37.456, 127.456
      );

      assertNotNull(result);
      verify(mockProfile, never()).updateDefaultAddressId(anyLong());
      verify(customerProfileRepository, never()).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void addAddressByProfileId_fail_profile_not_found() {
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      CustomerAddress result = customerProfileService.addAddressByProfileId(
          10L, "집", "서울시 강남구", 37.123, 127.123
      );

      assertNull(result);
      verify(customerAddressRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("배송지 수정 테스트")
  class UpdateAddressTest {

    @Test
    @DisplayName("성공 - 배송지 수정")
    void updateAddress_success() {
      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));
      when(customerAddressRepository.save(mockAddress)).thenReturn(mockAddress);

      boolean result = customerProfileService.updateAddress(
          10L, 1L, "새주소명", "새주소", 37.789, 127.789
      );

      assertTrue(result);
      verify(mockAddress, times(1)).updateAddress("새주소명", "새주소", 37.789, 127.789);
      verify(customerAddressRepository, times(1)).save(mockAddress);
    }

    @Test
    @DisplayName("실패 - 배송지 없음")
    void updateAddress_fail_not_found() {
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.empty());

      boolean result = customerProfileService.updateAddress(
          10L, 1L, "새주소명", "새주소", 37.789, 127.789
      );

      assertFalse(result);
      verify(customerAddressRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("배송지 삭제 테스트")
  class DeleteAddressTest {

    @Test
    @DisplayName("성공 - 배송지 삭제")
    void deleteAddress_success() {
      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(mockAddress.isDefault()).thenReturn(false);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));

      boolean result = customerProfileService.deleteAddress(1L, 1L);

      assertTrue(result);
      verify(customerAddressRepository, times(1)).delete(mockAddress);
    }

    @Test
    @DisplayName("실패 - 기본 배송지는 삭제 불가")
    void deleteAddress_fail_default_address() {
      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(mockAddress.isDefault()).thenReturn(true);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));

      boolean result = customerProfileService.deleteAddress(1L, 1L);

      assertFalse(result);
      verify(customerAddressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("실패 - 배송지 없음")
    void deleteAddress_fail_not_found() {
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.empty());

      boolean result = customerProfileService.deleteAddress(1L, 1L);

      assertFalse(result);
      verify(customerAddressRepository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("기본 배송지 설정 테스트")
  class SetDefaultAddressTest {

    @Test
    @DisplayName("성공 - 기본 배송지 설정")
    void setDefaultAddress_success() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(customerAddressRepository.findById(2L)).thenReturn(Optional.of(mockAddress));
      when(customerProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = customerProfileService.setDefaultAddress(10L, 2L);

      assertTrue(result);
      verify(mockProfile, times(1)).updateDefaultAddressId(2L);
      verify(customerProfileRepository, times(1)).save(mockProfile);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void setDefaultAddress_fail_profile_not_found() {
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      boolean result = customerProfileService.setDefaultAddress(10L, 2L);

      assertFalse(result);
      verify(customerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패 - 배송지 없음")
    void setDefaultAddress_fail_address_not_found() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(customerAddressRepository.findById(2L)).thenReturn(Optional.empty());

      boolean result = customerProfileService.setDefaultAddress(10L, 2L);

      assertFalse(result);
      verify(customerProfileRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("기본 배송지 조회 테스트")
  class GetCurrentAddressTest {

    @Test
    @DisplayName("성공 - 기본 배송지 조회")
    void getCurrentAddressByProfileId_success() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(mockProfile.getDefaultAddressId()).thenReturn(1L);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerAddress mockAddress = mock(CustomerAddress.class);
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.of(mockAddress));

      CustomerAddress result = customerProfileService.getCurrentAddress(10L);

      assertNotNull(result);
      assertEquals(mockAddress, result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void getCurrentAddressByProfileId_profile_not_found() {
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.empty());

      CustomerAddress result = customerProfileService.getCurrentAddress(10L);

      assertNull(result);
    }

    @Test
    @DisplayName("실패 - 기본 배송지 미설정")
    void getCurrentAddressByProfileId_no_default() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(mockProfile.getDefaultAddressId()).thenReturn(null);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));

      CustomerAddress result = customerProfileService.getCurrentAddress(10L);

      assertNull(result);
    }

    @Test
    @DisplayName("실패 - 기본 배송지 조회 실패")
    void getCurrentAddressByProfileId_address_not_found() {
      CustomerProfile mockProfile = mock(CustomerProfile.class);
      when(mockProfile.getDefaultAddressId()).thenReturn(1L);
      when(customerProfileRepository.findByProfileId(10L)).thenReturn(Optional.of(mockProfile));
      when(customerAddressRepository.findById(1L)).thenReturn(Optional.empty());

      CustomerAddress result = customerProfileService.getCurrentAddress(10L);

      assertNull(result);
    }
  }
}