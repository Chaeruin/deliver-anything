package com.deliveranything.domain.user.profile.controller;

import com.deliveranything.domain.user.profile.dto.seller.AccountInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.seller.BusinessInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileResponse;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileUpdateRequest;
import com.deliveranything.domain.user.profile.entity.SellerProfile;
import com.deliveranything.domain.user.profile.service.SellerProfileService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "판매자 프로필 관리 API", description = "seller profile 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/seller")
@RequiredArgsConstructor
@PreAuthorize("@profileSecurity.isSeller(authentication.principal)") //
public class SellerProfileController {

  private final SellerProfileService sellerProfileService;
  private final Rq rq;

  // ========== 프로필 관리 ==========

  @GetMapping
  @Operation(
      summary = "내 판매자 프로필 조회",
      description = "현재 활성화된 판매자 프로필의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<SellerProfileResponse>> getMyProfile() {
    Long profileId = rq.getCurrentProfileId();
    log.info("판매자 프로필 조회 요청: profileId={}", profileId);

    SellerProfile profile = sellerProfileService.getProfileByProfileId(profileId);
    if (profile == null) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    SellerProfileResponse response = SellerProfileResponse.from(profile);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping
  @Operation(
      summary = "내 판매자 프로필 수정",
      description = "판매자 프로필의 닉네임, 프로필 이미지를 수정합니다."
  )
  public ResponseEntity<ApiResponse<SellerProfileResponse>> updateMyProfile(
      @Valid @RequestBody SellerProfileUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("판매자 프로필 수정 요청: profileId={}", profileId);

    // 최소 하나의 필드는 입력되어야 함
    if ((request.nickname() == null || request.nickname().isBlank())
        && request.profileImageUrl() == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    boolean success = sellerProfileService.updateProfileByProfileId(
        profileId,
        request.nickname(),
        request.profileImageUrl()
    );

    if (!success) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    SellerProfile updatedProfile = sellerProfileService.getProfileByProfileId(profileId);
    SellerProfileResponse response = SellerProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("프로필이 수정되었습니다.", response)
    );
  }

  // ========== 사업자 정보 관리 ==========

  @PutMapping("/business-info")
  @Operation(
      summary = "사업자 정보 수정",
      description = "사업자명, 사업자 전화번호를 수정합니다."
  )
  public ResponseEntity<ApiResponse<SellerProfileResponse>> updateBusinessInfo(
      @Valid @RequestBody BusinessInfoUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("사업자 정보 수정 요청: profileId={}", profileId);

    // 최소 하나의 필드는 입력되어야 함
    if ((request.businessName() == null || request.businessName().isBlank())
        && (request.businessPhoneNumber() == null || request.businessPhoneNumber().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    boolean success = sellerProfileService.updateBusinessInfoByProfileId(
        profileId,
        request.businessName(),
        request.businessPhoneNumber()
    );

    if (!success) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    SellerProfile updatedProfile = sellerProfileService.getProfileByProfileId(profileId);
    SellerProfileResponse response = SellerProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("사업자 정보가 수정되었습니다.", response)
    );
  }

  // ========== 정산 계좌 정보 관리 ==========

  @PutMapping("/account-info")
  @Operation(
      summary = "정산 계좌 정보 수정",
      description = "은행명, 계좌번호, 예금주를 수정합니다."
  )
  public ResponseEntity<ApiResponse<SellerProfileResponse>> updateAccountInfo(
      @Valid @RequestBody AccountInfoUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("정산 계좌 정보 수정 요청: profileId={}", profileId);

    // 최소 하나의 필드는 입력되어야 함
    if ((request.bankName() == null || request.bankName().isBlank())
        && (request.accountNumber() == null || request.accountNumber().isBlank())
        && (request.accountHolder() == null || request.accountHolder().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    boolean success = sellerProfileService.updateBankInfoByProfileId(
        profileId,
        request.bankName(),
        request.accountNumber(),
        request.accountHolder()
    );

    if (!success) {
      throw new CustomException(ErrorCode.PROFILE_NOT_FOUND);
    }

    SellerProfile updatedProfile = sellerProfileService.getProfileByProfileId(profileId);
    SellerProfileResponse response = SellerProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("정산 계좌 정보가 수정되었습니다.", response)
    );
  }
}