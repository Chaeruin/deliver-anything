package com.deliveranything.domain.user.profile.controller;

import com.deliveranything.domain.user.profile.dto.customer.AddressCreateRequest;
import com.deliveranything.domain.user.profile.dto.customer.AddressResponse;
import com.deliveranything.domain.user.profile.dto.customer.AddressUpdateRequest;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileResponse;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileUpdateRequest;
import com.deliveranything.domain.user.profile.entity.CustomerAddress;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "고객 프로필 관리 API", description = "CustomerProfile 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/customer")
@RequiredArgsConstructor
public class CustomerProfileController {

  private final CustomerProfileService customerProfileService;
  private final Rq rq;

  // ========== 프로필 관리 ==========

  @GetMapping
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "내 고객 프로필 조회",
      description = "현재 활성화된 고객 프로필의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<CustomerProfileResponse>> getMyProfile(
      @AuthenticationPrincipal SecurityUser securityUser) {
    System.out.println("========== 컨트롤러 진입 =========="); // ✅ System.out으로 변경
    log.error("========== 컨트롤러 진입 =========="); // ✅ error 레벨로 변경
    Long profileId = securityUser.getCurrentActiveProfile().getId();

    // ✅ null 체크 추가
    if (profileId == null) {
      throw new CustomException(ErrorCode.PROFILE_REQUIRED);
    }

    log.info("고객 프로필 조회 요청: profileId={}", profileId);

    CustomerProfile profile = customerProfileService.getProfileByProfileId(profileId);
    if (profile == null) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    CustomerProfileResponse response = CustomerProfileResponse.from(profile);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "내 고객 프로필 수정",
      description = "고객 프로필의 닉네임, 프로필 이미지를 수정합니다."
  )
  public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateMyProfile(
      @AuthenticationPrincipal SecurityUser securityUser,
      @Valid @RequestBody CustomerProfileUpdateRequest request) {

    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("고객 프로필 수정 요청: profileId={}", profileId);

    // 최소 하나의 필드는 입력되어야 함
    if ((request.nickname() == null || request.nickname().isBlank())
        && request.profileImageUrl() == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    boolean success = customerProfileService.updateProfileByProfileId(
        profileId,
        request.nickname(),
        request.profileImageUrl()
    );

    if (!success) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    CustomerProfile updatedProfile = customerProfileService.getProfileByProfileId(profileId);
    CustomerProfileResponse response = CustomerProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("프로필이 수정되었습니다.", response)
    );
  }

  // ========== 배송지 관리 ==========

  @GetMapping("/addresses")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "내 배송지 목록 조회",
      description = "현재 고객 프로필에 등록된 모든 배송지를 조회합니다."
  )
  public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("배송지 목록 조회 요청: profileId={}", profileId);

    List<CustomerAddress> addresses = customerProfileService.getAddressesByProfileId(profileId);
    List<AddressResponse> response = addresses.stream()
        .map(AddressResponse::from)
        .toList();

    return ResponseEntity.ok(
        ApiResponse.success("배송지 목록 조회 완료", response)
    );
  }

  @GetMapping("/addresses/{addressId}")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "특정 배송지 조회",
      description = "배송지 ID로 특정 배송지의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long addressId) {

    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("배송지 조회 요청: profileId={}, addressId={}", profileId, addressId);

    CustomerAddress address = customerProfileService.getAddressByProfileId(profileId, addressId);
    if (address == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("ADDRESS-001", "배송지를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    AddressResponse response = AddressResponse.from(address);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PostMapping("/addresses")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "배송지 추가",
      description = "새로운 배송지를 추가합니다. 첫 번째 배송지는 자동으로 기본 배송지로 설정됩니다."
  )
  public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
      @AuthenticationPrincipal SecurityUser securityUser,
      @Valid @RequestBody AddressCreateRequest request) {

    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("배송지 추가 요청: profileId={}", profileId);

    CustomerAddress address = customerProfileService.addAddressByProfileId(
        profileId,
        request.addressName(),
        request.address(),
        request.latitude(),
        request.longitude()
    );

    if (address == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("ADDRESS-002", "배송지 추가에 실패했습니다."));
    }

    AddressResponse response = AddressResponse.from(address);
    return ResponseEntity.ok(
        ApiResponse.success("배송지가 추가되었습니다.", response)
    );
  }

  @PutMapping("/addresses/{addressId}")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "배송지 수정",
      description = "기존 배송지의 정보를 수정합니다."
  )
  public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long addressId,
      @Valid @RequestBody AddressUpdateRequest request) {

    // 🔴 수정됨: userId 대신 profileId 사용
    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("배송지 수정 요청: profileId={}, addressId={}", profileId, addressId);

    // 최소 하나의 필드는 입력되어야 함
    if ((request.addressName() == null || request.addressName().isBlank())
        && (request.address() == null || request.address().isBlank())
        && request.latitude() == null
        && request.longitude() == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    // 🔴 수정됨: userId 대신 profileId 전달
    boolean success = customerProfileService.updateAddress(
        profileId,
        addressId,
        request.addressName(),
        request.address(),
        request.latitude(),
        request.longitude()
    );

    if (!success) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("ADDRESS-003", "배송지 수정에 실패했습니다."));
    }

    CustomerAddress updatedAddress = customerProfileService.getAddress(addressId, 1L);
    AddressResponse response = AddressResponse.from(updatedAddress);

    return ResponseEntity.ok(
        ApiResponse.success("배송지가 수정되었습니다.", response)
    );
  }

  @DeleteMapping("/addresses/{addressId}")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "배송지 삭제",
      description = "배송지를 삭제합니다. 기본 배송지는 삭제할 수 없습니다."
  )
  public ResponseEntity<ApiResponse<Void>> deleteAddress(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long addressId) {

    // ✅ 주의: Service 메서드가 userId를 받도록 설계되어 있어 이 부분은 유지합니다.
    Long userId = rq.getActor().getId();
    log.info("배송지 삭제 요청: userId={}, addressId={}", userId, addressId);

    boolean success = customerProfileService.deleteAddress(userId, addressId);

    if (!success) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("ADDRESS-004", "배송지 삭제에 실패했습니다. 기본 배송지는 삭제할 수 없습니다."));
    }

    return ResponseEntity.ok(
        ApiResponse.success("배송지가 삭제되었습니다.", null)
    );
  }

  @PutMapping("/addresses/{addressId}/default")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "기본 배송지 설정",
      description = "특정 배송지를 기본 배송지로 설정합니다."
  )
  public ResponseEntity<ApiResponse<Void>> setDefaultAddress(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long addressId) {

    // 🔴 수정됨: userId 대신 profileId 사용
    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("기본 배송지 설정 요청: profileId={}, addressId={}", profileId, addressId);

    // 🔴 수정됨: userId 대신 profileId 전달
    boolean success = customerProfileService.setDefaultAddress(profileId, addressId);

    if (!success) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("ADDRESS-005", "기본 배송지 설정에 실패했습니다."));
    }

    return ResponseEntity.ok(
        ApiResponse.success("기본 배송지가 설정되었습니다.", null)
    );
  }

  @GetMapping("/addresses/default")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  @Operation(
      summary = "기본 배송지 조회",
      description = "현재 설정된 기본 배송지를 조회합니다."
  )
  public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    Long profileId = securityUser.getCurrentActiveProfile().getId();
    log.info("기본 배송지 조회 요청: profileId={}", profileId);

    CustomerAddress defaultAddress = customerProfileService.getCurrentAddress(profileId);
    if (defaultAddress == null) {
      return ResponseEntity.ok(
          ApiResponse.success("설정된 기본 배송지가 없습니다.", null)
      );
    }

    AddressResponse response = AddressResponse.from(defaultAddress);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
}