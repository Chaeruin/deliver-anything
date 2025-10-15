package com.deliveranything.domain.user.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.enums.RiderToggleStatus;
import com.deliveranything.domain.user.profile.repository.RiderProfileRepository;
import com.deliveranything.global.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderProfileService 단위 테스트")
class RiderProfileServiceTest {

  @Mock
  private RiderProfileRepository riderProfileRepository;

  @InjectMocks
  private RiderProfileService riderProfileService;

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - profileId로 프로필 조회")
    void getRiderProfileById_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      RiderProfile result = riderProfileService.getRiderProfileById(10L);

      assertNotNull(result);
      assertEquals(mockProfile, result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void getRiderProfileById_not_found() {
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.empty());

      assertThrows(CustomException.class, () -> {
        riderProfileService.getRiderProfileById(10L);
      });
    }
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 프로필 수정")
    void updateProfileByProfileId_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));
      when(riderProfileRepository.save(mockProfile)).thenReturn(mockProfile);

      boolean result = riderProfileService.updateProfileByProfileId(10L, "새닉네임",
          "http://new-image.url");

      assertTrue(result);
      verify(mockProfile, times(1)).updateProfile("새닉네임", "http://new-image.url");
      verify(riderProfileRepository, times(1)).save(mockProfile);
    }
  }

  @Nested
  @DisplayName("배달 상태 토글 테스트")
  class ToggleStatusTest {

    @Test
    @DisplayName("성공 - 배달 상태 토글")
    void toggleDeliveryStatus_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      riderProfileService.toggleDeliveryStatus(10L);

      verify(mockProfile, times(1)).toggleStatus();
      verify(riderProfileRepository, times(1)).save(mockProfile);
    }
  }

  @Nested
  @DisplayName("배달 상태 설정 테스트")
  class UpdateStatusTest {

    @Test
    @DisplayName("성공 - 배달 상태 설정")
    void updateDeliveryStatus_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      riderProfileService.updateDeliveryStatus(10L, "ON");

      verify(mockProfile, times(1)).updateToggleStatus((RiderToggleStatus) any());
      verify(riderProfileRepository, times(1)).save(mockProfile);
    }
  }

  @Nested
  @DisplayName("배달 가능 여부 확인 테스트")
  class IsAvailableTest {

    @Test
    @DisplayName("성공 - 배달 가능")
    void isAvailableForDelivery_true() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.isAvailableForDelivery()).thenReturn(true);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      boolean result = riderProfileService.isAvailableForDelivery(10L);

      assertTrue(result);
    }

    @Test
    @DisplayName("성공 - 배달 불가능")
    void isAvailableForDelivery_false() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.isAvailableForDelivery()).thenReturn(false);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      boolean result = riderProfileService.isAvailableForDelivery(10L);

      assertFalse(result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void isAvailableForDelivery_profile_not_found() {
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.empty());

      assertThrows(CustomException.class, () -> {
        riderProfileService.isAvailableForDelivery(10L);
      });
    }
  }

  @Nested
  @DisplayName("활동 지역 수정 테스트")
  class UpdateAreaTest {

    @Test
    @DisplayName("성공 - 활동 지역 수정")
    void updateDeliveryArea_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      riderProfileService.updateDeliveryArea(10L, "경기도");

      verify(mockProfile, times(1)).updateDeliveryArea("경기도");
      verify(riderProfileRepository, times(1)).save(mockProfile);
    }
  }

  @Nested
  @DisplayName("활동 지역 조회 테스트")
  class GetAreaTest {

    @Test
    @DisplayName("성공 - 활동 지역 조회")
    void getDeliveryArea_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getArea()).thenReturn("서울시");
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      String result = riderProfileService.getDeliveryArea(10L);

      assertEquals("서울시", result);
    }

    @Test
    @DisplayName("실패 - 프로필 없음")
    void getDeliveryArea_not_found() {
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.empty());

      assertThrows(CustomException.class, () -> {
        riderProfileService.getDeliveryArea(10L);
      });
    }
  }

  @Nested
  @DisplayName("계좌 정보 수정 테스트")
  class UpdateBankInfoTest {

    @Test
    @DisplayName("성공 - 계좌 정보 수정")
    void updateBankInfo_success() {
      RiderProfile mockProfile = mock(RiderProfile.class);
      when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(mockProfile));

      riderProfileService.updateBankInfo(10L, "신한은행", "110-123-456789", "홍길동");

      verify(mockProfile, times(1)).updateBankInfo("신한은행", "110-123-456789", "홍길동");
      verify(riderProfileRepository, times(1)).save(mockProfile);
    }
  }
}