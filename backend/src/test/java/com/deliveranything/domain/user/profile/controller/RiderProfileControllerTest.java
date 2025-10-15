package com.deliveranything.domain.user.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.dto.rider.RiderAccountInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderAreaUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderStatusUpdateRequest;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.enums.RiderToggleStatus;
import com.deliveranything.domain.user.profile.service.RiderProfileService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderProfileController 단위 테스트")
class RiderProfileControllerTest {

  @Mock
  private RiderProfileService riderProfileService;

  @Mock
  private Rq rq;

  @InjectMocks
  private RiderProfileController riderProfileController;

  private SecurityUser createMockSecurityUser(Long userId, Long profileId) {
    Profile mockProfile = mock(Profile.class);
    when(mockProfile.getId()).thenReturn(profileId);

    return new SecurityUser(
        userId,
        "라이더",
        "password",
        "rider@test.com",
        mockProfile,
        List.of(new SimpleGrantedAuthority("ROLE_RIDER"))
    );
  }

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - 라이더 프로필 조회")
    void getMyProfile_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getId()).thenReturn(10L);
      when(mockProfile.getNickname()).thenReturn("라이더닉네임");
      when(mockProfile.getToggleStatus()).thenReturn(RiderToggleStatus.OFF);
      when(mockProfile.getArea()).thenReturn("서울시");

      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.getMyProfile();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(riderProfileService, times(1)).getRiderProfileById(10L);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void getMyProfile_fail_not_found() {
      when(rq.getCurrentProfileId()).thenReturn(10L);
      when(riderProfileService.getRiderProfileById(10L))
          .thenThrow(new CustomException(ErrorCode.PROFILE_NOT_FOUND));

      assertThrows(CustomException.class, () -> {
        riderProfileController.getMyProfile();
      });
    }
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 닉네임 수정")
    void updateMyProfile_success_nickname() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderProfileUpdateRequest request = new RiderProfileUpdateRequest("새닉네임", null);

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getNickname()).thenReturn("새닉네임");

      when(riderProfileService.updateProfileByProfileId(10L, "새닉네임", null)).thenReturn(true);
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필이 수정되었습니다.", body.getMessage());

      verify(riderProfileService, times(1)).updateProfileByProfileId(10L, "새닉네임", null);
    }

    @Test
    @DisplayName("성공 - 프로필 이미지 수정")
    void updateMyProfile_success_image() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderProfileUpdateRequest request = new RiderProfileUpdateRequest(
          null, "http://new-image.url"
      );

      RiderProfile mockProfile = mock(RiderProfile.class);

      when(riderProfileService.updateProfileByProfileId(10L, null,
          "http://new-image.url")).thenReturn(true);
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(riderProfileService, times(1))
          .updateProfileByProfileId(10L, null, "http://new-image.url");
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateMyProfile_fail_no_data() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderProfileUpdateRequest request = new RiderProfileUpdateRequest(null, null);

      ResponseEntity<?> response = riderProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(riderProfileService, never())
          .updateProfileByProfileId(anyLong(), anyString(), anyString());
    }
  }

  @Nested
  @DisplayName("배달 상태 토글 테스트")
  class ToggleStatusTest {

    @Test
    @DisplayName("성공 - 배달 상태 토글")
    void toggleDeliveryStatus_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getToggleStatus()).thenReturn(RiderToggleStatus.ON);

      doNothing().when(riderProfileService).toggleDeliveryStatus(10L);
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.toggleDeliveryStatus();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("배달 상태가 변경되었습니다.", body.getMessage());

      verify(riderProfileService, times(1)).toggleDeliveryStatus(10L);
    }
  }

  @Nested
  @DisplayName("배달 상태 설정 테스트")
  class UpdateStatusTest {

    @Test
    @DisplayName("성공 - 배달 상태 ON 설정")
    void updateDeliveryStatus_success_on() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderStatusUpdateRequest request = new RiderStatusUpdateRequest("ON");

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getToggleStatus()).thenReturn(RiderToggleStatus.ON);

      doNothing().when(riderProfileService).updateDeliveryStatus(10L, "ON");
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateDeliveryStatus(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("배달 상태가 변경되었습니다.", body.getMessage());

      verify(riderProfileService, times(1)).updateDeliveryStatus(10L, "ON");
    }

    @Test
    @DisplayName("성공 - 배달 상태 OFF 설정")
    void updateDeliveryStatus_success_off() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderStatusUpdateRequest request = new RiderStatusUpdateRequest("OFF");

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getToggleStatus()).thenReturn(RiderToggleStatus.OFF);

      doNothing().when(riderProfileService).updateDeliveryStatus(10L, "OFF");
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateDeliveryStatus(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(riderProfileService, times(1)).updateDeliveryStatus(10L, "OFF");
    }
  }

  @Nested
  @DisplayName("배달 가능 여부 조회 테스트")
  class CheckAvailabilityTest {

    @Test
    @DisplayName("성공 - 배달 가능")
    void checkAvailability_available() {
      when(rq.getCurrentProfileId()).thenReturn(10L);
      when(riderProfileService.isAvailableForDelivery(10L)).thenReturn(true);

      ResponseEntity<?> response = riderProfileController.checkAvailability();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("배달 가능 여부 조회 완료", body.getMessage());

      verify(riderProfileService, times(1)).isAvailableForDelivery(10L);
    }

    @Test
    @DisplayName("성공 - 배달 불가능")
    void checkAvailability_not_available() {
      when(rq.getCurrentProfileId()).thenReturn(10L);
      when(riderProfileService.isAvailableForDelivery(10L)).thenReturn(false);

      ResponseEntity<?> response = riderProfileController.checkAvailability();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(riderProfileService, times(1)).isAvailableForDelivery(10L);
    }
  }

  @Nested
  @DisplayName("활동 지역 수정 테스트")
  class UpdateAreaTest {

    @Test
    @DisplayName("성공 - 활동 지역 수정")
    void updateDeliveryArea_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderAreaUpdateRequest request = new RiderAreaUpdateRequest("경기도 수원시");

      RiderProfile mockProfile = mock(RiderProfile.class);
      when(mockProfile.getArea()).thenReturn("경기도 수원시");

      doNothing().when(riderProfileService).updateDeliveryArea(10L, "경기도 수원시");
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateDeliveryArea(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("활동 지역이 수정되었습니다.", body.getMessage());

      verify(riderProfileService, times(1)).updateDeliveryArea(10L, "경기도 수원시");
    }
  }

  @Nested
  @DisplayName("활동 지역 조회 테스트")
  class GetAreaTest {

    @Test
    @DisplayName("성공 - 활동 지역 조회")
    void getDeliveryArea_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);
      when(riderProfileService.getDeliveryArea(10L)).thenReturn("서울시 강남구");

      ResponseEntity<?> response = riderProfileController.getDeliveryArea();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("활동 지역 조회 완료", body.getMessage());

      verify(riderProfileService, times(1)).getDeliveryArea(10L);
    }
  }

  @Nested
  @DisplayName("계좌 정보 수정 테스트")
  class UpdateAccountInfoTest {

    @Test
    @DisplayName("성공 - 계좌 정보 수정")
    void updateAccountInfo_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderAccountInfoUpdateRequest request = new RiderAccountInfoUpdateRequest(
          "신한은행", "110-123-456789", "홍길동"
      );

      RiderProfile mockProfile = mock(RiderProfile.class);

      doNothing().when(riderProfileService)
          .updateBankInfo(10L, "신한은행", "110-123-456789", "홍길동");
      when(riderProfileService.getRiderProfileById(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = riderProfileController.updateAccountInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("정산 계좌 정보가 수정되었습니다.", body.getMessage());

      verify(riderProfileService, times(1))
          .updateBankInfo(10L, "신한은행", "110-123-456789", "홍길동");
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateAccountInfo_fail_no_data() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      RiderAccountInfoUpdateRequest request = new RiderAccountInfoUpdateRequest(null, null, null);

      ResponseEntity<?> response = riderProfileController.updateAccountInfo(request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(riderProfileService, never())
          .updateBankInfo(anyLong(), anyString(), anyString(), anyString());
    }
  }
}