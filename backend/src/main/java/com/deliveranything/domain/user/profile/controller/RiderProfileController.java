package com.deliveranything.domain.user.profile.controller;

import com.deliveranything.domain.user.profile.dto.rider.RiderAccountInfoUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderAreaUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileResponse;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileUpdateRequest;
import com.deliveranything.domain.user.profile.dto.rider.RiderStatusUpdateRequest;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.service.RiderProfileService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배달원 프로필 관리 API", description = "RiderProfile 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/rider")
@RequiredArgsConstructor
@PreAuthorize("@profileSecurity.isRider(authentication.principal)")
public class RiderProfileController {

  private final RiderProfileService riderProfileService;
  private final Rq rq;

  // ========== 프로필 관리 ==========

  @GetMapping
  @Operation(
      summary = "내 배달원 프로필 조회",
      description = "현재 활성화된 배달원 프로필의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> getMyProfile() {
    Long profileId = rq.getCurrentProfileId();
    log.info("배달원 프로필 조회 요청: profileId={}", profileId);

    RiderProfile profile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(profile);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping
  @Operation(
      summary = "내 배달원 프로필 수정",
      description = "배달원 프로필의 닉네임, 프로필 이미지를 수정합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> updateMyProfile(
      @Valid @RequestBody RiderProfileUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("배달원 프로필 수정 요청: profileId={}", profileId);

    if ((request.nickname() == null || request.nickname().isBlank())
        && request.profileImageUrl() == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    riderProfileService.updateProfileByProfileId(
        profileId,
        request.nickname(),
        request.profileImageUrl()
    );

    RiderProfile updatedProfile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("프로필이 수정되었습니다.", response)
    );
  }

  // ========== 배달 상태 관리 ==========

  @PostMapping("/toggle")
  @Operation(
      summary = "배달 상태 토글",
      description = "배달 상태를 ON ↔ OFF로 전환합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> toggleDeliveryStatus() {
    Long profileId = rq.getCurrentProfileId();
    log.info("배달 상태 토글 요청: profileId={}", profileId);

    riderProfileService.toggleDeliveryStatus(profileId);

    RiderProfile updatedProfile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("배달 상태가 변경되었습니다.", response)
    );
  }

  @PutMapping("/status")
  @Operation(
      summary = "배달 상태 설정",
      description = "배달 상태를 ON 또는 OFF로 직접 설정합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> updateDeliveryStatus(
      @Valid @RequestBody RiderStatusUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("배달 상태 변경 요청: profileId={}, status={}", profileId, request.riderStatus());

    riderProfileService.updateDeliveryStatus(profileId, request.riderStatus());

    RiderProfile updatedProfile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("배달 상태가 변경되었습니다.", response)
    );
  }

  @GetMapping("/available")
  @Operation(
      summary = "배달 가능 여부 조회",
      description = "현재 배달 가능한 상태인지 확인합니다. (프로필 활성화 + 배달 상태 ON)"
  )
  public ResponseEntity<ApiResponse<Boolean>> checkAvailability() {
    Long profileId = rq.getCurrentProfileId();
    log.info("배달 가능 여부 조회: profileId={}", profileId);

    boolean isAvailable = riderProfileService.isAvailableForDelivery(profileId);

    return ResponseEntity.ok(
        ApiResponse.success("배달 가능 여부 조회 완료", isAvailable)
    );
  }

  // ========== 활동 지역 관리 ==========

  @PutMapping("/area")
  @Operation(
      summary = "활동 지역 수정",
      description = "배달원의 활동 지역을 변경합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> updateDeliveryArea(
      @Valid @RequestBody RiderAreaUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("활동 지역 수정 요청: profileId={}, area={}", profileId, request.deliveryArea());

    riderProfileService.updateDeliveryArea(profileId, request.deliveryArea());

    RiderProfile updatedProfile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("활동 지역이 수정되었습니다.", response)
    );
  }

  @GetMapping("/area")
  @Operation(
      summary = "활동 지역 조회",
      description = "현재 설정된 활동 지역을 조회합니다."
  )
  public ResponseEntity<ApiResponse<String>> getDeliveryArea() {
    Long profileId = rq.getCurrentProfileId();
    log.info("활동 지역 조회: profileId={}", profileId);

    String area = riderProfileService.getDeliveryArea(profileId);

    return ResponseEntity.ok(
        ApiResponse.success("활동 지역 조회 완료", area)
    );
  }

  // ========== 정산 계좌 정보 관리 ==========

  @PutMapping("/account-info")
  @Operation(
      summary = "정산 계좌 정보 수정",
      description = "은행명, 계좌번호, 예금주를 수정합니다."
  )
  public ResponseEntity<ApiResponse<RiderProfileResponse>> updateAccountInfo(
      @Valid @RequestBody RiderAccountInfoUpdateRequest request) {

    Long profileId = rq.getCurrentProfileId();
    log.info("정산 계좌 정보 수정 요청: profileId={}", profileId);

    if ((request.bankName() == null || request.bankName().isBlank())
        && (request.bankAccountNumber() == null || request.bankAccountNumber().isBlank())
        && (request.bankAccountHolderName() == null || request.bankAccountHolderName().isBlank())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("VALIDATION-001", "수정할 정보를 입력해주세요."));
    }

    riderProfileService.updateBankInfo(
        profileId,
        request.bankName(),
        request.bankAccountNumber(),
        request.bankAccountHolderName()
    );

    RiderProfile updatedProfile = riderProfileService.getRiderProfileById(profileId);
    RiderProfileResponse response = RiderProfileResponse.from(updatedProfile);

    return ResponseEntity.ok(
        ApiResponse.success("정산 계좌 정보가 수정되었습니다.", response)
    );
  }
}