package com.deliveranything.domain.user.user.controller;

import com.deliveranything.domain.auth.service.AuthService;
import com.deliveranything.domain.user.profile.dto.AvailableProfilesResponse;
import com.deliveranything.domain.user.profile.dto.CreateProfileRequest;
import com.deliveranything.domain.user.profile.dto.ProfileResponse;
import com.deliveranything.domain.user.profile.dto.SwitchProfileRequest;
import com.deliveranything.domain.user.profile.dto.SwitchProfileResponse;
import com.deliveranything.domain.user.profile.dto.customer.CustomerProfileCreateData;
import com.deliveranything.domain.user.profile.dto.rider.RiderProfileCreateData;
import com.deliveranything.domain.user.profile.dto.seller.SellerProfileCreateData;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 프로필 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

  private final ProfileService profileService;
  private final AuthService authService;
  private final UserService userService;
  private final Rq rq;

  // ========== 기본 사용자 정보 ==========

  @GetMapping
  @Operation(
      summary = "내 정보 조회",
      description = "현재 로그인한 사용자의 상세 정보를 조회합니다."
  )
  public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo() {
    User currentUser = rq.getActor();
    log.info("사용자 정보 조회: userId={}", currentUser.getId());

    UserInfoResponse response = UserInfoResponse.from(currentUser);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping
  @Operation(
      summary = "내 정보 수정",
      description = "사용자 이름, 전화번호 등 기본 정보를 수정합니다."
  )
  public ResponseEntity<ApiResponse<UserInfoResponse>> updateMyInfo(
      @Valid @RequestBody UpdateUserRequest request) {

    User currentUser = rq.getActor();
    log.info("사용자 정보 수정 요청: userId={}", currentUser.getId());

    User updatedUser = userService.updateUserInfo(
        currentUser.getId(),
        request.username(),
        request.phoneNumber()
    );

    UserInfoResponse response = UserInfoResponse.from(updatedUser);

    return ResponseEntity.ok(
        ApiResponse.success("사용자 정보가 수정되었습니다.", response)
    );
  }

  @PutMapping("/password")
  @Operation(
      summary = "비밀번호 변경",
      description = "현재 비밀번호를 확인하고 새로운 비밀번호로 변경합니다."
  )
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {

    User currentUser = rq.getActor();
    log.info("비밀번호 변경 요청: userId={}", currentUser.getId());

    userService.changePassword(
        currentUser.getId(),
        request.currentPassword(),
        request.newPassword()
    );

    return ResponseEntity.ok(
        ApiResponse.success("비밀번호가 변경되었습니다.", null)
    );
  }

  // ========== ✅ 프로필 관리 (온보딩 통합) ==========

  @PostMapping("/profiles")
  @Operation(
      summary = "프로필 생성",
      description = "새로운 프로필을 생성합니다. 첫 번째 프로필이든 추가 프로필이든 동일하게 처리됩니다."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "프로필 타입과 상세 정보를 입력합니다.",
      required = true,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = CreateProfileRequest.class),
          examples = {
              @ExampleObject(
                  name = "CUSTOMER",
                  summary = "소비자 프로필",
                  description = "소비자 프로필 생성 (주소는 나중에 별도 추가)",
                  value = """
                      {
                        "profileType": "CUSTOMER",
                        "profileData": {
                          "nickname": "홍길동",
                          "profileImageUrl": null,
                          "customerPhoneNumber": "010-1234-5678"
                        }
                      }
                      """
              ),
              @ExampleObject(
                  name = "SELLER",
                  summary = "판매자 프로필",
                  description = "판매자 프로필은 사업자 정보가 필요합니다",
                  value = """
                      {
                        "profileType": "SELLER",
                        "profileData": {
                          "nickname": "홍사장",
                          "businessName": "홍길동식당",
                          "businessCertificateNumber": "123-45-67890",
                          "businessPhoneNumber": "02-1234-5678",
                          "bankName": "신한은행",
                          "accountNumber": "1234567890123",
                          "accountHolder": "홍길동",
                          "profileImageUrl": null
                        }
                      }
                      """
              ),
              @ExampleObject(
                  name = "RIDER",
                  summary = "배달원 프로필",
                  description = "배달원 프로필은 차량 정보가 필요합니다",
                  value = """
                      {
                        "profileType": "RIDER",
                        "profileData": {
                          "nickname": "김배달",
                          "vehicleType": "MOTORCYCLE",
                          "vehicleNumber": "12가3456",
                          "licenseNumber": "12-34-567890-12",
                          "bankName": "국민은행",
                          "accountNumber": "9876543210987",
                          "accountHolder": "김배달",
                          "profileImageUrl": null
                        }
                      }
                      """
              )
          }
      )
  )
  public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
      @Valid @RequestBody CreateProfileRequest request) {

    User currentUser = rq.getActor();
    log.info("프로필 생성 요청: userId={}, profileType={}",
        currentUser.getId(), request.profileType());

    // 프로필 생성
    Profile newProfile = profileService.createProfile(
        currentUser,
        request.profileType(),
        request.profileData()
    );

    // 응답 생성
    ProfileResponse response = ProfileResponse.builder()
        .userId(currentUser.getId())
        .profileType(request.profileType())
        .profileId(newProfile.getId())
        .nickname(getNicknameFromProfileData(request.profileData()))
        .isActive(newProfile.isActive())
        .message(String.format("%s 프로필이 생성되었습니다.",
            request.profileType().getDescription()))
        .build();

    return ResponseEntity.ok(
        ApiResponse.success("프로필이 생성되었습니다.", response)
    );
  }

  @PostMapping("/profile/switch")
  @Operation(
      summary = "프로필 전환",
      description = "사용자가 보유한 다른 프로필로 전환합니다. 프로필 전환 시 새로운 Access Token이 자동으로 발급되며, "
                    + "판매자 프로필인 경우 storeId와 프로필 상세 정보도 함께 반환됩니다."
  )
  public ResponseEntity<ApiResponse<SwitchProfileResponse>> switchProfile(
      @Valid @RequestBody SwitchProfileRequest request,
      @RequestHeader("Authorization") String authorization,
      @RequestHeader("User-Agent") String deviceId) { // deviceId 추가

    User currentUser = rq.getActor();
    log.info("프로필 전환 요청: userId={}, targetProfile={}, deviceId={}", // 로그 추가
        currentUser.getId(), request.targetProfileType(), deviceId);

    // 기존 Access Token 추출
    String oldAccessToken = authorization.replace("Bearer ", "");

    // AuthService가 프로필 전환 + 토큰 재발급 + storeId + 프로필 상세 조회
    SwitchProfileResponse result = authService.switchProfileWithTokenReissue(
        currentUser.getId(),
        request.targetProfileType(),
        oldAccessToken, // 기존 토큰 전달
        deviceId // deviceId 전달
    );

    // 새 Access Token을 쿠키와 헤더에 설정
    rq.setAccessToken(result.accessToken());

    log.info("프로필 전환 완료 및 Access Token 재발급: userId={}, {} -> {}",
        currentUser.getId(), result.previousProfileType(), result.currentProfileType());

    // storeId + 프로필 상세 정보 포함된 API 응답용으로 변환 (토큰 제거)
    SwitchProfileResponse response = result.toResponse();

    return ResponseEntity.ok(
        ApiResponse.success("프로필이 전환되었습니다.", response)
    );
  }

  @GetMapping("/profiles")
  @Operation(
      summary = "사용 가능한 프로필 목록 조회",
      description = "현재 사용자가 보유한 모든 활성 프로필 목록과 현재 활성화된 프로필을 조회합니다."
  )
  public ResponseEntity<ApiResponse<AvailableProfilesResponse>> getAvailableProfiles() {

    User currentUser = rq.getActor();
    log.info("사용 가능한 프로필 조회: userId={}", currentUser.getId());

    List<ProfileType> availableProfiles = profileService.getAvailableProfiles(
        currentUser
    );

    AvailableProfilesResponse response = AvailableProfilesResponse.builder()
        .userId(currentUser.getId())
        .availableProfiles(availableProfiles)
        .currentActiveProfile(currentUser.getCurrentActiveProfileType())
        .build();

    return ResponseEntity.ok(
        ApiResponse.success("프로필 목록 조회 완료", response)
    );
  }

  // ========== 헬퍼 메서드 ==========

  /**
   * profileData에서 nickname 추출
   */
  private String getNicknameFromProfileData(Object profileData) {
    if (profileData instanceof CustomerProfileCreateData data) {
      return data.nickname();
    } else if (profileData instanceof SellerProfileCreateData data) {
      return data.nickname();
    } else if (profileData instanceof RiderProfileCreateData data) {
      return data.nickname();
    }
    return "Unknown";
  }
}