package com.deliveranything.domain.user.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.dto.SwitchProfileResult;
import com.deliveranything.domain.auth.service.AuthService;
import com.deliveranything.domain.user.profile.dto.AvailableProfilesResponse;
import com.deliveranything.domain.user.profile.dto.CreateProfileRequest;
import com.deliveranything.domain.user.profile.dto.CreateProfileResponse;
import com.deliveranything.domain.user.profile.dto.SwitchProfileRequest;
import com.deliveranything.domain.user.profile.dto.SwitchProfileResponse;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileCreateData;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.service.ProfileService;
import com.deliveranything.domain.user.user.dto.ChangePasswordRequest;
import com.deliveranything.domain.user.user.dto.UpdateUserRequest;
import com.deliveranything.domain.user.user.dto.UserInfoResponse;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.service.UserService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 단위 테스트")
class UserControllerTest {

  @Mock
  private ProfileService profileService;

  @Mock
  private AuthService authService;

  @Mock
  private UserService userService;

  @Mock
  private Rq rq;

  @InjectMocks
  private UserController userController;

  @Nested
  @DisplayName("사용자 정보 조회 테스트")
  class GetMyInfoTest {

    @Test
    @DisplayName("성공 - 내 정보 조회")
    void getMyInfo_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getEmail()).thenReturn("test@test.com");
      when(mockUser.getUsername()).thenReturn("테스트");
      when(rq.getActor()).thenReturn(mockUser);

      ResponseEntity<ApiResponse<UserInfoResponse>> response = userController.getMyInfo();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
    }
  }

  @Nested
  @DisplayName("사용자 정보 수정 테스트")
  class UpdateMyInfoTest {

    @Test
    @DisplayName("성공 - 내 정보 수정")
    void updateMyInfo_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(rq.getActor()).thenReturn(mockUser);

      User updatedUser = mock(User.class);
      when(updatedUser.getId()).thenReturn(1L);
      when(updatedUser.getUsername()).thenReturn("새이름");
      when(userService.updateUserInfo(1L, "새이름", "01087654321")).thenReturn(updatedUser);

      UpdateUserRequest request = new UpdateUserRequest("새이름", "01087654321");

      ResponseEntity<ApiResponse<UserInfoResponse>> response = userController.updateMyInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("사용자 정보가 수정되었습니다.", body.getMessage());

      verify(userService, times(1)).updateUserInfo(1L, "새이름", "01087654321");
    }
  }

  @Nested
  @DisplayName("비밀번호 변경 테스트")
  class ChangePasswordTest {

    @Test
    @DisplayName("성공 - 비밀번호 변경")
    void changePassword_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(rq.getActor()).thenReturn(mockUser);

      doNothing().when(userService).changePassword(1L, "oldPassword", "newPassword");

      ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword");

      ResponseEntity<ApiResponse<Void>> response = userController.changePassword(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<Void> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("비밀번호가 변경되었습니다.", body.getMessage());

      verify(userService, times(1)).changePassword(1L, "oldPassword", "newPassword");
    }
  }

  @Nested
  @DisplayName("프로필 생성 테스트")
  class CreateProfileTest {

    @Test
    @DisplayName("성공 - 프로필 생성")
    void createProfile_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(rq.getActor()).thenReturn(mockUser);

      Profile mockProfile = mock(Profile.class);
      when(mockProfile.getId()).thenReturn(10L);
      when(mockProfile.isActive()).thenReturn(true);

      CustomerProfileCreateData profileData = new CustomerProfileCreateData(
          "닉네임", null, "01012345678"
      );

      CreateProfileRequest request = new CreateProfileRequest(ProfileType.CUSTOMER, profileData);

      when(profileService.createProfile(mockUser, ProfileType.CUSTOMER, profileData))
          .thenReturn(mockProfile);

      ResponseEntity<ApiResponse<CreateProfileResponse>> response = userController.createProfile(
          request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필이 생성되었습니다.", body.getMessage());

      verify(profileService, times(1)).createProfile(mockUser, ProfileType.CUSTOMER, profileData);
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
      when(rq.getActor()).thenReturn(mockUser);

      SwitchProfileRequest request = new SwitchProfileRequest(ProfileType.SELLER);

      SwitchProfileResponse switchProfileResponse = SwitchProfileResponse.builder()
          .userId(1L)
          .previousProfileType(ProfileType.CUSTOMER)
          .currentProfileType(ProfileType.SELLER)
          .currentProfileId(20L)
          .storeId(100L)
          .build();

      SwitchProfileResult switchResult = new SwitchProfileResult(
          switchProfileResponse,
          "newAccessToken"
      );

      when(authService.switchProfileWithTokenReissue(
          1L, ProfileType.SELLER, "oldToken", "device123"
      )).thenReturn(switchResult);

      ResponseEntity<ApiResponse<SwitchProfileResponse>> response = userController.switchProfile(
          request,
          "Bearer oldToken",
          "device123"
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<SwitchProfileResponse> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필이 전환되었습니다.", body.getMessage());

      verify(authService, times(1))
          .switchProfileWithTokenReissue(1L, ProfileType.SELLER, "oldToken", "device123");
      verify(rq, times(1)).setAccessToken("newAccessToken");
    }
  }

  @Nested
  @DisplayName("사용 가능한 프로필 조회 테스트")
  class GetAvailableProfilesTest {

    @Test
    @DisplayName("성공 - 사용 가능한 프로필 조회")
    void getAvailableProfiles_success() {
      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(mockUser.getCurrentActiveProfileType()).thenReturn(ProfileType.CUSTOMER);
      when(rq.getActor()).thenReturn(mockUser);

      List<ProfileType> availableProfiles = List.of(ProfileType.CUSTOMER, ProfileType.SELLER);
      when(profileService.getAvailableProfiles(mockUser)).thenReturn(availableProfiles);

      ResponseEntity<ApiResponse<AvailableProfilesResponse>> response = userController.getAvailableProfiles();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필 목록 조회 완료", body.getMessage());

      verify(profileService, times(1)).getAvailableProfiles(mockUser);
    }
  }
}