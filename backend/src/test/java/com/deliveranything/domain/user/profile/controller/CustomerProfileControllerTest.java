package com.deliveranything.domain.user.profile.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.user.profile.dto.customer.AddressCreateRequest;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileUpdateRequest;
import com.deliveranything.domain.user.profile.entity.CustomerAddress;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.security.auth.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerProfileController 단위 테스트")
class CustomerProfileControllerTest {

  @Mock
  private CustomerProfileService customerProfileService;

  @Mock
  private Rq rq;

  @InjectMocks
  private CustomerProfileController customerProfileController;

  private SecurityUser createMockSecurityUser(Long userId, Long profileId) {
    Profile mockProfile = mock(Profile.class);
    lenient().when(mockProfile.getId()).thenReturn(profileId);

    return new SecurityUser(
        userId,
        "테스트유저",
        "password",
        "test@test.com",
        mockProfile,
        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
    );
  }

  private CustomerAddress createMockAddress(Long id, String name, String address, double lat,
      double lon) {
    CustomerAddress mockAddress = mock(CustomerAddress.class);
    // ReflectionTestUtils 사용 대신 Mockito로 ID 스텁
    when(mockAddress.getId()).thenReturn(id);
    when(mockAddress.getAddressName()).thenReturn(name);
    when(mockAddress.getAddress()).thenReturn(address);

    // 새로운 Location Mock 객체 생성
    Point mockPoint = mock(Point.class);
    when(mockPoint.getY()).thenReturn(lat); // 위도 (Latitude)
    when(mockPoint.getX()).thenReturn(lon); // 경도 (Longitude)

    // CustomerAddress.getLocation()이 Mock Point를 반환하도록 스텁
    when(mockAddress.getLocation()).thenReturn(mockPoint);

    return mockAddress;
  }

  @Nested
  @DisplayName("프로필 조회 테스트")
  class GetProfileTest {

    @Test
    @DisplayName("성공 - 고객 프로필 조회")
    void getMyProfile_success() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      CustomerProfile mockCustomerProfile = mock(CustomerProfile.class);
      when(mockCustomerProfile.getId()).thenReturn(10L);
      when(mockCustomerProfile.getNickname()).thenReturn("고객닉네임");
      when(mockCustomerProfile.getProfileImageUrl()).thenReturn("http://image.url");

      when(customerProfileService.getProfileByProfileId(10L)).thenReturn(mockCustomerProfile);

      ResponseEntity<?> response = customerProfileController.getMyProfile(mockSecurityUser);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(customerProfileService, times(1)).getProfileByProfileId(10L);
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void getMyProfile_fail_not_found() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      when(customerProfileService.getProfileByProfileId(10L)).thenReturn(null);

      assertThrows(CustomException.class, () -> {
        customerProfileController.getMyProfile(mockSecurityUser);
      });

      verify(customerProfileService, times(1)).getProfileByProfileId(10L);
    }

    @Test
    @DisplayName("실패 - profileId가 null")
    void getMyProfile_fail_null_profile_id() {
      Profile mockProfile = mock(Profile.class);
      when(mockProfile.getId()).thenReturn(null);

      SecurityUser mockSecurityUser = new SecurityUser(
          1L, "테스트", "password", "test@test.com", mockProfile,
          List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
      );

      assertThrows(CustomException.class, () -> {
        customerProfileController.getMyProfile(mockSecurityUser);
      });
    }
  }

  @Nested
  @DisplayName("프로필 수정 테스트")
  class UpdateProfileTest {

    @Test
    @DisplayName("성공 - 닉네임 수정")
    void updateMyProfile_success_nickname() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("새닉네임", null);

      CustomerProfile mockCustomerProfile = mock(CustomerProfile.class);
      when(mockCustomerProfile.getNickname()).thenReturn("새닉네임");

      when(customerProfileService.updateProfileByProfileId(10L, "새닉네임", null))
          .thenReturn(true);
      when(customerProfileService.getProfileByProfileId(10L)).thenReturn(mockCustomerProfile);

      ResponseEntity<?> response = customerProfileController.updateMyProfile(
          mockSecurityUser, request
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("프로필이 수정되었습니다.", body.getMessage());

      verify(customerProfileService, times(1))
          .updateProfileByProfileId(10L, "새닉네임", null);
    }

    @Test
    @DisplayName("성공 - 프로필 이미지 수정")
    void updateMyProfile_success_image() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest(
          null, "http://new-image.url"
      );

      CustomerProfile mockCustomerProfile = mock(CustomerProfile.class);

      when(customerProfileService.updateProfileByProfileId(10L, null, "http://new-image.url"))
          .thenReturn(true);
      when(customerProfileService.getProfileByProfileId(10L)).thenReturn(mockCustomerProfile);

      ResponseEntity<?> response = customerProfileController.updateMyProfile(
          mockSecurityUser, request
      );

      assertEquals(HttpStatus.OK, response.getStatusCode());

      verify(customerProfileService, times(1))
          .updateProfileByProfileId(10L, null, "http://new-image.url");
    }

    @Test
    @DisplayName("실패 - 수정할 정보 없음")
    void updateMyProfile_fail_no_data() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest(null, null);

      ResponseEntity<?> response = customerProfileController.updateMyProfile(
          mockSecurityUser, request
      );

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("VALIDATION-001", body.getCode());

      verify(customerProfileService, never()).updateProfileByProfileId(anyLong(), anyString(),
          anyString());
    }

    @Test
    @DisplayName("실패 - 프로필을 찾을 수 없음")
    void updateMyProfile_fail_not_found() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("새닉네임", null);

      when(customerProfileService.updateProfileByProfileId(10L, "새닉네임", null))
          .thenReturn(false);

      assertThrows(CustomException.class, () -> {
        customerProfileController.updateMyProfile(mockSecurityUser, request);
      });
    }
  }

  @Nested
  @DisplayName("배송지 목록 조회 테스트")
  class GetAddressesTest {

    @Test
    @DisplayName("성공 - 배송지 목록 조회")
    void getMyAddresses_success() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      CustomerAddress address1 = createMockAddress(1L, "집", "서울시 강남구", 37.1, 127.1);
      CustomerAddress address2 = createMockAddress(2L, "회사", "서울시 서초구", 37.2, 127.2);

      when(customerProfileService.getAddressesByProfileId(10L))
          .thenReturn(List.of(address1, address2));

      ResponseEntity<?> response = customerProfileController.getMyAddresses(mockSecurityUser);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());

      verify(customerProfileService, times(1)).getAddressesByProfileId(10L);
    }

    @Test
    @DisplayName("성공 - 배송지 없음")
    void getMyAddresses_empty() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      when(customerProfileService.getAddressesByProfileId(10L))
          .thenReturn(List.of());

      ResponseEntity<?> response = customerProfileController.getMyAddresses(mockSecurityUser);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
    }
  }

  @Nested
  @DisplayName("배송지 추가 테스트")
  class AddAddressTest {

    @Test
    @DisplayName("성공 - 배송지 추가")
    void addAddress_success() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      AddressCreateRequest request = new AddressCreateRequest(
          "집", "서울시 강남구", 37.123456, 127.123456
      );

      CustomerAddress mockAddress = createMockAddress(1L, "집", "서울시 강남구", 37.123456, 127.123456);

      when(mockAddress.getAddressName()).thenReturn("집");

      when(customerProfileService.addAddressByProfileId(
          10L, "집", "서울시 강남구", 37.123456, 127.123456
      )).thenReturn(mockAddress);

      ResponseEntity<?> response = customerProfileController.addAddress(mockSecurityUser, request);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("배송지가 추가되었습니다.", body.getMessage());

      verify(customerProfileService, times(1))
          .addAddressByProfileId(10L, "집", "서울시 강남구", 37.123456, 127.123456);
    }

    @Test
    @DisplayName("실패 - 배송지 추가 실패")
    void addAddress_fail() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);
      AddressCreateRequest request = new AddressCreateRequest(
          "집", "서울시 강남구", 37.123456, 127.123456
      );

      when(customerProfileService.addAddressByProfileId(
          anyLong(), anyString(), anyString(), anyDouble(), anyDouble()
      )).thenReturn(null);

      ResponseEntity<?> response = customerProfileController.addAddress(mockSecurityUser, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("ADDRESS-002", body.getCode());
    }
  }

  @Nested
  @DisplayName("배송지 삭제 테스트")
  class DeleteAddressTest {

    @Test
    @DisplayName("성공 - 배송지 삭제")
    void deleteAddress_success() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(rq.getActor()).thenReturn(mockUser);
      when(customerProfileService.deleteAddress(1L, 1L)).thenReturn(true);

      ResponseEntity<?> response = customerProfileController.deleteAddress(mockSecurityUser, 1L);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("배송지가 삭제되었습니다.", body.getMessage());

      verify(customerProfileService, times(1)).deleteAddress(1L, 1L);
    }

    @Test
    @DisplayName("실패 - 기본 배송지는 삭제 불가")
    void deleteAddress_fail_default_address() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      User mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      when(rq.getActor()).thenReturn(mockUser);
      when(customerProfileService.deleteAddress(1L, 1L)).thenReturn(false);

      ResponseEntity<?> response = customerProfileController.deleteAddress(mockSecurityUser, 1L);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertFalse(body.isSuccess());
      assertEquals("ADDRESS-004", body.getCode());
    }
  }

  @Nested
  @DisplayName("기본 배송지 조회 테스트")
  class GetDefaultAddressTest {

    @Test
    @DisplayName("성공 - 기본 배송지 조회")
    void getDefaultAddress_success() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      CustomerAddress mockAddress = createMockAddress(1L, "집", "서울시 강남구", 37.123456, 127.123456);
      when(customerProfileService.getCurrentAddress(10L))
          .thenReturn(mockAddress);

      ResponseEntity<?> response = customerProfileController.getDefaultAddress(mockSecurityUser);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
    }

    @Test
    @DisplayName("성공 - 기본 배송지 없음")
    void getDefaultAddress_not_set() {
      SecurityUser mockSecurityUser = createMockSecurityUser(1L, 10L);

      when(customerProfileService.getCurrentAddress(10L))
          .thenReturn(null);

      ResponseEntity<?> response = customerProfileController.getDefaultAddress(mockSecurityUser);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      ApiResponse<?> body = (ApiResponse<?>) response.getBody();
      assertNotNull(body);
      assertTrue(body.isSuccess());
      assertEquals("설정된 기본 배송지가 없습니다.", body.getMessage());
    }
  }
}