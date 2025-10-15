package com.deliveranything.domain.user.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.dto.seller.AccountInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.seller.BusinessInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileUpdateRequest;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.service.SellerProfileService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
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
@DisplayName("SellerProfileController 단위 테스트")
class SellerProfileControllerTest {

  @Mock
  private SellerProfileService sellerProfileService;

  @Mock
  private Rq rq;

  @InjectMocks
  private SellerProfileController sellerProfileController;

  private SecurityUser createMockSecurityUser(Long userId, Long profileId) {
    Profile mockProfile = mock(Profile.class);
    when(mockProfile.getId()).thenReturn(profileId);

    return new SecurityUser(
        userId,
        "판매자",
        "password",
        "seller@test.com",
        mockProfile,
        List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
    );
  }

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - 판매자 프로필 조회")
    void getMyProfile_success() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      SellerProfile mockProfile = mock(SellerProfile.class);
      when(mockProfile.getId()).thenReturn(10L);
      when(mockProfile.getNickname()).thenReturn("판매자닉네임");
      when(mockProfile.getBusinessName()).thenReturn("사업자명");

      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.getMyProfile();

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(sellerProfileService, times(1)).getProfileByProfileId(10L);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void getMyProfile_fail_not_found() {
      when(rq.getCurrentProfileId()).thenReturn(10L);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(null);

      assertThrows(CustomException.class, () -> {
        sellerProfileController.getMyProfile();
      });

      verify(sellerProfileService, times(1)).getProfileByProfileId(10L);
    }
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 닉네임 수정")
    void updateMyProfile_success_nickname() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      SellerProfileUpdateRequest request = new SellerProfileUpdateRequest("새닉네임", null);

      SellerProfile mockProfile = mock(SellerProfile.class);
      when(mockProfile.getNickname()).thenReturn("새닉네임");

      when(sellerProfileService.updateProfileByProfileId(10L, "새닉네임", null))
          .thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필이 수정되었습니다.", body.getMessage());

      verify(sellerProfileService, times(1)).updateProfileByProfileId(10L, "새닉네임", null);
    }

    @Test
    @DisplayName("성공 - 프로필 이미지 수정")
    void updateMyProfile_success_image() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      SellerProfileUpdateRequest request = new SellerProfileUpdateRequest(
          null, "http://new-image.url"
      );

      SellerProfile mockProfile = mock(SellerProfile.class);

      when(sellerProfileService.updateProfileByProfileId(10L, null, "http://new-image.url"))
          .thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(sellerProfileService, times(1))
          .updateProfileByProfileId(10L, null, "http://new-image.url");
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateMyProfile_fail_no_data() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      SellerProfileUpdateRequest request = new SellerProfileUpdateRequest(null, null);

      ResponseEntity<?> response = sellerProfileController.updateMyProfile(request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(sellerProfileService, never())
          .updateProfileByProfileId(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateMyProfile_fail_not_found() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      SellerProfileUpdateRequest request = new SellerProfileUpdateRequest("새닉네임", null);

      when(sellerProfileService.updateProfileByProfileId(10L, "새닉네임", null))
          .thenReturn(false);

      assertThrows(CustomException.class, () -> {
        sellerProfileController.updateMyProfile(request);
      });
    }
  }

  @Nested
  @DisplayName("사업자 정보 수정 테스트")
  class UpdateBusinessInfoTest {

    @Test
    @DisplayName("성공 - 사업자명 수정")
    void updateBusinessInfo_success_name() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      BusinessInfoUpdateRequest request = new BusinessInfoUpdateRequest("새사업자명", null);

      SellerProfile mockProfile = mock(SellerProfile.class);
      when(mockProfile.getBusinessName()).thenReturn("새사업자명");

      when(sellerProfileService.updateBusinessInfoByProfileId(10L, "새사업자명", null))
          .thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateBusinessInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("사업자 정보가 수정되었습니다.", body.getMessage());

      verify(sellerProfileService, times(1))
          .updateBusinessInfoByProfileId(10L, "새사업자명", null);
    }

    @Test
    @DisplayName("성공 - 사업자 전화번호 수정")
    void updateBusinessInfo_success_phone() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      BusinessInfoUpdateRequest request = new BusinessInfoUpdateRequest(null, "02-1234-5678");

      SellerProfile mockProfile = mock(SellerProfile.class);

      when(sellerProfileService.updateBusinessInfoByProfileId(10L, null, "02-1234-5678"))
          .thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateBusinessInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(sellerProfileService, times(1))
          .updateBusinessInfoByProfileId(10L, null, "02-1234-5678");
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateBusinessInfo_fail_no_data() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      BusinessInfoUpdateRequest request = new BusinessInfoUpdateRequest(null, null);

      ResponseEntity<?> response = sellerProfileController.updateBusinessInfo(request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(sellerProfileService, never())
          .updateBusinessInfoByProfileId(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateBusinessInfo_fail_not_found() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      BusinessInfoUpdateRequest request = new BusinessInfoUpdateRequest("새사업자명", null);

      when(sellerProfileService.updateBusinessInfoByProfileId(10L, "새사업자명", null))
          .thenReturn(false);

      assertThrows(CustomException.class, () -> {
        sellerProfileController.updateBusinessInfo(request);
      });
    }
  }

  @Nested
  @DisplayName("계좌 정보 수정 테스트")
  class UpdateAccountInfoTest {

    @Test
    @DisplayName("성공 - 계좌 정보 전체 수정")
    void updateAccountInfo_success_all() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      AccountInfoUpdateRequest request = new AccountInfoUpdateRequest(
          "신한은행", "110-123-456789", "홍길동"
      );

      SellerProfile mockProfile = mock(SellerProfile.class);

      when(sellerProfileService.updateBankInfoByProfileId(
          10L, "신한은행", "110-123-456789", "홍길동"
      )).thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateAccountInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("정산 계좌 정보가 수정되었습니다.", body.getMessage());

      verify(sellerProfileService, times(1))
          .updateBankInfoByProfileId(10L, "신한은행", "110-123-456789", "홍길동");
    }

    @Test
    @DisplayName("성공 - 은행명만 수정")
    void updateAccountInfo_success_bank_only() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      AccountInfoUpdateRequest request = new AccountInfoUpdateRequest("국민은행", null, null);

      SellerProfile mockProfile = mock(SellerProfile.class);

      when(sellerProfileService.updateBankInfoByProfileId(10L, "국민은행", null, null))
          .thenReturn(true);
      when(sellerProfileService.getProfileByProfileId(10L)).thenReturn(mockProfile);

      ResponseEntity<?> response = sellerProfileController.updateAccountInfo(request);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(sellerProfileService, times(1))
          .updateBankInfoByProfileId(10L, "국민은행", null, null);
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateAccountInfo_fail_no_data() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      AccountInfoUpdateRequest request = new AccountInfoUpdateRequest(null, null, null);

      ResponseEntity<?> response = sellerProfileController.updateAccountInfo(request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(sellerProfileService, never())
          .updateBankInfoByProfileId(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateAccountInfo_fail_not_found() {
      when(rq.getCurrentProfileId()).thenReturn(10L);

      AccountInfoUpdateRequest request = new AccountInfoUpdateRequest(
          "신한은행", "110-123-456789", "홍길동"
      );

      when(sellerProfileService.updateBankInfoByProfileId(
          10L, "신한은행", "110-123-456789", "홍길동"
      )).thenReturn(false);

      assertThrows(CustomException.class, () -> {
        sellerProfileController.updateAccountInfo(request);
      });
    }
  }
}