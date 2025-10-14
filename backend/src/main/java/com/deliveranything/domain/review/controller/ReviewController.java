package com.deliveranything.domain.review.controller;

import com.deliveranything.domain.review.dto.ReviewCreateRequest;
import com.deliveranything.domain.review.dto.ReviewCreateResponse;
import com.deliveranything.domain.review.dto.ReviewLikeResponse;
import com.deliveranything.domain.review.dto.ReviewListRequest;
import com.deliveranything.domain.review.dto.ReviewRatingAndListResponseDto;
import com.deliveranything.domain.review.dto.ReviewResponse;
import com.deliveranything.domain.review.dto.ReviewUpdateRequest;
import com.deliveranything.domain.review.dto.StoreReviewListRequest;
import com.deliveranything.domain.review.service.ReviewService;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "Review", description = "리뷰 관련 API")
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping("api/v1/reviews")
  @Operation(summary = "리뷰 생성", description = "새로운 리뷰를 생성합니다.")
  public ResponseEntity<ApiResponse<ReviewCreateResponse>> createReview(
      @RequestBody @Valid ReviewCreateRequest request,
      @AuthenticationPrincipal SecurityUser user
  ) {
    if (!user.hasActiveProfile(ProfileType.CUSTOMER)) {
      throw new CustomException(ErrorCode.PROFILE_NOT_ALLOWED);
    }

    ReviewCreateResponse response = reviewService.createReview(request, user.getId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response));
  }

  @DeleteMapping("api/v1/reviews/{reviewId}")
  @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다.")
  public ResponseEntity<ApiResponse<String>> deleteReview(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long reviewId
  ) {
    if (!user.hasActiveProfile(ProfileType.CUSTOMER)) {
      throw new CustomException(ErrorCode.PROFILE_NOT_ALLOWED);
    }

    reviewService.deleteReview(user.getId(), reviewId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(ApiResponse.success(null));
  }

  @PatchMapping("api/v1/reviews/{reviewId}")
  @Operation(summary = "리뷰 수정", description = "리뷰를 수정합니다.")
  public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long reviewId,
      @RequestBody ReviewUpdateRequest request
  ) {
    if (!user.hasActiveProfile(ProfileType.CUSTOMER)) {
      throw new CustomException(ErrorCode.PROFILE_NOT_ALLOWED);
    }

    ReviewResponse response = reviewService.updateReview(request, reviewId, user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  @GetMapping("api/v1/reviews/{reviewId}")
  @Operation(summary = "리뷰 조회", description = "리뷰 id로 리뷰를 조회합니다.")
  public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
      @PathVariable Long reviewId
  ) {
    ReviewResponse response = reviewService.getReview(reviewId);

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  @GetMapping("api/v1/me/reviews")
  @Operation(summary = "내 리뷰 리스트 & 평점 조회", description = "sort, cursor, size와 사용자의 프로필에 따라 작성한 리뷰 or 내게 달린 리뷰 리스트 및 평균 평점을 조회합니다.")
  public ResponseEntity<ApiResponse<ReviewRatingAndListResponseDto>> getMyReviews(
      @AuthenticationPrincipal SecurityUser user,
      @RequestBody ReviewListRequest request
  ) {
    ReviewRatingAndListResponseDto response = reviewService.getMyReviews(user.getCurrentActiveProfileIdSafe(), user.getCurrentActiveProfile(), request.sort(), request.cursor(), request.size());

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  @GetMapping("api/v1/stores/{storeId}/reviews")
  @Operation(summary = "특정 상점 리뷰 리스트 & 평점 조회", description = "sort, cursor, size 를 받아 특정 상점의 리뷰 리스트 및 평균 평점을 조회합니다.")
  @PreAuthorize("hasRole('USER')")
  public  ResponseEntity<ApiResponse<ReviewRatingAndListResponseDto>> getStoreReviews(
      @PathVariable Long storeId,
      @ModelAttribute StoreReviewListRequest request) {
    ReviewRatingAndListResponseDto response = reviewService.getStoreReviews(storeId, request.sort(), request.cursor(), request.size());

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  @PostMapping("api/v1/reviews/{reviewId}/like")
  @Operation(summary = "리뷰 좋아요 등록", description = "로그인한 사용자가 특정 리뷰에 좋아요를 누릅니다. 이미 좋아요한 리뷰는 중복 등록할 수 없습니다.")
  public ResponseEntity<ApiResponse<ReviewLikeResponse>> likeReview(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long reviewId
  ) {
    ReviewLikeResponse response = reviewService.likeReview(reviewId, user.getId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response));
  }

  @DeleteMapping("api/v1/reviews/{reviewId}/like")
  @Operation(summary = "리뷰 좋아요 취소", description = "로그인한 사용자가 특정 리뷰에 좋아요를 누른 상태에서 다시 좋아요를 누르면 취소됩니다.")
  public ResponseEntity<ApiResponse<ReviewLikeResponse>> unLikeReview(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long reviewId
  ) {
    ReviewLikeResponse response = reviewService.unlikeReview(reviewId, user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }

  @GetMapping("api/v1/reviews/{reviewId}/likes")
  @Operation(summary = "리뷰 좋아요 수 조회", description = "특정 리뷰에 달린 좋아요의 수를 조회합니다.")
  public ResponseEntity<ApiResponse<ReviewLikeResponse>> getReviewLikeCount(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long reviewId
  ) {
    ReviewLikeResponse response = reviewService.getReviewLikeCount(reviewId, user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(response));
  }
}
