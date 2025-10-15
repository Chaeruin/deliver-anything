package com.deliveranything.domain.user.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileCreateData;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileCreateData;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileCreateData;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService 단위 테스트")
class ProfileServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private CustomerProfileRepository customerProfileRepository;

  @Mock
  private SellerProfileRepository sellerProfileRepository;

  @Mock
  private RiderProfileRepository riderProfileRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ProfileService profileService;

  @Nested
  @DisplayName("프로필 생성 테스트")
  class CreateProfileTest {

    @Test
    @DisplayName("성공 - 첫 번째 고객 프로필 생성")
    void createProfile_first_customer_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(false);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.CUSTOMER)).thenReturn(false);

      Profile savedProfile = Profile.builder()
          .user(mockUser)
          .type(ProfileType.CUSTOMER)
          .build();

      when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);

      CustomerProfileCreateData profileData = new CustomerProfileCreateData(
          "닉네임", null, "01012345678"
      );

      Profile result = profileService.createProfile(mockUser, ProfileType.CUSTOMER, profileData);

      assertNotNull(result);
      verify(profileRepository, times(1)).save(any(Profile.class));
      verify(customerProfileRepository, times(1)).save(any(CustomerProfile.class));
      verify(mockUser, times(1)).setCurrentActiveProfile(any(Profile.class));
      verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("성공 - 추가 판매자 프로필 생성")
    void createProfile_additional_seller_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.SELLER)).thenReturn(false);
      when(sellerProfileRepository.existsByBusinessCertificateNumber(anyString()))
          .thenReturn(false);

      Profile savedProfile = Profile.builder()
          .user(mockUser)
          .type(ProfileType.SELLER)
          .build();

      when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);

      SellerProfileCreateData profileData = new SellerProfileCreateData(
          "판매자", null, "사업자명", "123-45-67890", "02-1234-5678",
          "신한은행", "1234567890", "홍길동"
      );

      Profile result = profileService.createProfile(mockUser, ProfileType.SELLER, profileData);

      assertNotNull(result);
      verify(profileRepository, times(1)).save(any(Profile.class));
      verify(sellerProfileRepository, times(1)).save(any(SellerProfile.class));
      verify(mockUser, never()).setCurrentActiveProfile(any());
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공 - 라이더 프로필 생성")
    void createProfile_rider_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(false);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.RIDER)).thenReturn(false);

      Profile savedProfile = Profile.builder()
          .user(mockUser)
          .type(ProfileType.RIDER)
          .build();

      when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);

      RiderProfileCreateData profileData = new RiderProfileCreateData(
          "라이더", null, "서울시", "12345"
      );

      Profile result = profileService.createProfile(mockUser, ProfileType.RIDER, profileData);

      assertNotNull(result);
      verify(profileRepository, times(1)).save(any(Profile.class));
      verify(riderProfileRepository, times(1)).save(any(RiderProfile.class));
    }

    @Test
    @DisplayName("실패 - 이미 존재하는 프로필 타입")
    void createProfile_fail_already_exists() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.CUSTOMER)).thenReturn(true);

      CustomerProfileCreateData profileData = new CustomerProfileCreateData(
          "닉네임", null, "01012345678"
      );

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.createProfile(mockUser, ProfileType.CUSTOMER, profileData);
      });

      assertEquals(ErrorCode.PROFILE_ALREADY_EXISTS.getCode(), exception.getCode());
      verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패 - 사업자등록번호 중복")
    void createProfile_fail_duplicate_business_certificate() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.SELLER)).thenReturn(false);

      Profile savedProfile = Profile.builder()
          .user(mockUser)
          .type(ProfileType.SELLER)
          .build();

      when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);
      when(sellerProfileRepository.existsByBusinessCertificateNumber(anyString()))
          .thenReturn(true);

      SellerProfileCreateData profileData = new SellerProfileCreateData(
          "판매자", null, "사업자명", "123-45-67890", "02-1234-5678",
          "신한은행", "1234567890", "홍길동"
      );

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.createProfile(mockUser, ProfileType.SELLER, profileData);
      });

      assertEquals(ErrorCode.BUSINESS_CERTIFICATE_DUPLICATE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 잘못된 프로필 데이터 타입")
    void createProfile_fail_invalid_profile_data_type() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      when(profileRepository.existsByUserIdAndType(1L, ProfileType.CUSTOMER)).thenReturn(false);

      Profile savedProfile = Profile.builder()
          .user(mockUser)
          .type(ProfileType.CUSTOMER)
          .build();

      when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);

      String invalidData = "invalid";

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.createProfile(mockUser, ProfileType.CUSTOMER, invalidData);
      });

      assertEquals(ErrorCode.PROFILE_NOT_FOUND.getCode(), exception.getCode());
    }
  }

  @Nested
  @DisplayName("프로필 전환 테스트")
  class SwitchProfileTest {

    @Test
    @DisplayName("성공 - 프로필 전환")
    void switchProfile_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);
      when(mockUser.getCurrentActiveProfileId()).thenReturn(10L);

      Profile targetProfile = mock(Profile.class);
      when(targetProfile.getId()).thenReturn(20L);
      when(targetProfile.isActive()).thenReturn(true);

      when(profileRepository.findByUserIdAndType(1L, ProfileType.SELLER))
          .thenReturn(Optional.of(targetProfile));

      boolean result = profileService.switchProfile(mockUser, ProfileType.SELLER, "device123");

      assertTrue(result);
      verify(mockUser, times(1)).switchProfile(targetProfile);
      verify(userRepository, times(1)).save(mockUser);
      verify(eventPublisher, times(1)).publishEvent(any(ActiveProfileChangedEvent.class));
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void switchProfile_fail_no_profile() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(false);

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.switchProfile(mockUser, ProfileType.SELLER, "device123");
      });

      assertEquals(ErrorCode.PROFILE_REQUIRED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 타겟 프로필을 찾을 수 없음")
    void switchProfile_fail_target_not_found() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);

      when(profileRepository.findByUserIdAndType(1L, ProfileType.SELLER))
          .thenReturn(Optional.empty());

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.switchProfile(mockUser, ProfileType.SELLER, "device123");
      });

      assertEquals(ErrorCode.PROFILE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 이미 활성화된 프로필")
    void switchProfile_fail_already_active() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);

      Profile targetProfile = mock(Profile.class);
      when(profileRepository.findByUserIdAndType(1L, ProfileType.CUSTOMER))
          .thenReturn(Optional.of(targetProfile));

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.switchProfile(mockUser, ProfileType.CUSTOMER, "device123");
      });

      assertEquals(ErrorCode.PROFILE_ALREADY_ACTIVE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 비활성화된 프로필")
    void switchProfile_fail_inactive_profile() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);

      Profile targetProfile = mock(Profile.class);
      when(targetProfile.isActive()).thenReturn(false);

      when(profileRepository.findByUserIdAndType(1L, ProfileType.SELLER))
          .thenReturn(Optional.of(targetProfile));

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.switchProfile(mockUser, ProfileType.SELLER, "device123");
      });

      assertEquals(ErrorCode.PROFILE_INACTIVE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("실패 - 프로필 전환 중 예외 발생")
    void switchProfile_fail_exception_during_switch() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.hasActiveProfile()).thenReturn(true);
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);

      Profile targetProfile = mock(Profile.class);
      when(targetProfile.isActive()).thenReturn(true);

      when(profileRepository.findByUserIdAndType(1L, ProfileType.SELLER))
          .thenReturn(Optional.of(targetProfile));

      doThrow(new IllegalStateException("전환 실패"))
          .when(mockUser).switchProfile(targetProfile);

      CustomException exception = assertThrows(CustomException.class, () -> {
        profileService.switchProfile(mockUser, ProfileType.SELLER, "device123");
      });

      assertEquals(ErrorCode.PROFILE_NOT_ALLOWED.getCode(), exception.getCode());
    }
  }

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - 사용 가능한 프로필 조회")
    void getAvailableProfiles_success() {
      User mockUser = mock(User.class);
      List<ProfileType> mockProfiles = List.of(ProfileType.CUSTOMER, ProfileType.SELLER);
      when(mockUser.getActiveProfileTypes()).thenReturn(mockProfiles);

      List<ProfileType> result = profileService.getAvailableProfiles(mockUser);

      assertNotNull(result);
      assertEquals(2, result.size());
      assertTrue(result.contains(ProfileType.CUSTOMER));
      assertTrue(result.contains(ProfileType.SELLER));
    }

    @Test
    @DisplayName("성공 - 사용자와 타입으로 프로필 조회")
    void getProfileByUserAndType_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);

      Profile mockProfile = mock(Profile.class);
      when(profileRepository.findByUserIdAndType(1L, ProfileType.CUSTOMER))
          .thenReturn(Optional.of(mockProfile));

      Profile result = profileService.getProfileByUserAndType(mockUser, ProfileType.CUSTOMER);

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }

    @Test
    @DisplayName("성공 - 활성 프로필 조회")
    void getActiveProfilesByUser_success() {
      User mockUser = mock(User.class);

      Profile profile1 = mock(Profile.class);
      Profile profile2 = mock(Profile.class);
      List<Profile> mockProfiles = List.of(profile1, profile2);

      when(profileRepository.findActiveProfilesByUser(mockUser)).thenReturn(mockProfiles);

      List<Profile> result = profileService.getActiveProfilesByUser(mockUser);

      assertNotNull(result);
      assertEquals(2, result.size());
    }
  }
}