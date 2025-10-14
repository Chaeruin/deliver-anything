package com.deliveranything.domain.review.entity;

import com.deliveranything.domain.review.dto.ReviewCreateRequest;
import com.deliveranything.domain.review.dto.ReviewUpdateRequest;
import com.deliveranything.domain.review.enums.ReviewTargetType;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

  @Schema(description = "별점")
  @Column(nullable = false)
  private Integer rating;

  @Schema(description = "리뷰 코멘트")
  @Column(length = 1000)
  private String comment;

  @Schema(description = "대상 타입(상점 or 배달원)")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReviewTargetType targetType;

  @Schema(description = "대상 ID (판매자 or 배달원)")
  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Schema(description = "작성자 객체")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_profile_id", nullable = false)
  private CustomerProfile customerProfile;

  @Schema(description = "리뷰 사진")
  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ReviewPhoto> reviewPhotos = new ArrayList<>();

  @LastModifiedDate
  @Schema(description = "수정 일시")
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Schema(description = "좋아요 수")
  @Column(name = "like_count")
  private int likeCount = 0;

  //========================생성 메소드===========================
  @Builder
  public Review(int rating, String comment, ReviewTargetType targetType, Long targetId,
      CustomerProfile customerProfile, List<ReviewPhoto> reviewPhotos) {
    this.rating = rating;
    this.comment = comment;
    this.reviewPhotos = reviewPhotos;
    this.targetType = targetType;
    this.targetId = targetId;
    this.customerProfile = customerProfile;
  }

  public static Review from(ReviewCreateRequest request, CustomerProfile customerProfile) {
    List<ReviewPhoto> photos = Arrays.stream(request.photoUrls())
        .map(url -> ReviewPhoto.builder()
            .photoUrl(url)
            .build())
        .collect(Collectors.toList());

    return Review.builder()
        .rating(request.rating())
        .comment(request.comment())
        .targetType(request.targetType())
        .targetId(request.targetId())
        .customerProfile(customerProfile)
        .reviewPhotos(photos)
        .build();
  }

  //===========================편의 메소드=======================
  public void update(ReviewUpdateRequest request) {
    if (request.rating() != null) {
      this.rating = request.rating();
    }

    if (request.comment() != null) {
      this.comment = request.comment();
    }
  }

  public void updateReviewPhoto(String[] photoUrls) {
    if (photoUrls != null) {
      this.getReviewPhotos().clear();

      for (String url : photoUrls) {
        ReviewPhoto photo = ReviewPhoto.builder()
            .review(this)
            .photoUrl(url)
            .build();
        this.getReviewPhotos().add(photo);
      }
    }
  }
}
