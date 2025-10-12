package com.deliveranything.domain.review.service;

import com.deliveranything.domain.notification.entity.Notification;
import com.deliveranything.domain.notification.enums.NotificationType;
import com.deliveranything.domain.notification.repository.NotificationRepository;
import com.deliveranything.domain.review.dto.ReviewCreateRequest;
import com.deliveranything.domain.review.dto.ReviewCreateResponse;
import com.deliveranything.domain.review.dto.ReviewLikeResponse;
import com.deliveranything.domain.review.dto.ReviewRatingAndListResponseDto;
import com.deliveranything.domain.review.dto.ReviewResponse;
import com.deliveranything.domain.review.dto.ReviewUpdateRequest;
import com.deliveranything.domain.review.entity.Review;
import com.deliveranything.domain.review.entity.ReviewPhoto;
import com.deliveranything.domain.review.enums.LikeAction;
import com.deliveranything.domain.review.enums.MyReviewSortType;
import com.deliveranything.domain.review.enums.ReviewTargetType;
import com.deliveranything.domain.review.enums.StoreReviewSortType;
import com.deliveranything.domain.review.repository.ReviewPhotoRepository;
import com.deliveranything.domain.review.repository.ReviewRepository;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.service.UserService;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.util.CursorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewPhotoRepository reviewPhotoRepository;
  private final UserService userService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final StoreService storeService;
  private final CustomerProfileService customerProfileService;
  private final NotificationRepository notificationRepository;

  //============================메인 API 메서드==================================
  /* 리뷰 생성 */
  public ReviewCreateResponse createReview(ReviewCreateRequest request, Long userId) {
    log.info("리뷰 생성 요청 - userId: {}, request: {}", userId, request);
    /***
     *  2025-09-29 수정 사항 - daran2
     *  유저를 통해 프로필을 받아오는 방식 -> 프로필을 받고 프로필에서 커스터머 프로필을 받아오는 방식으로 변경
     *  이유: 유저가 여러 프로필을 가질 수 있기 때문에, 변경된 프로필 구조에선 유저를 통해 커스터머 프로필을 바로 받아오는 것은 부적절
     *
     *  2025 09-29 수정2 - daran2
     *  customerProfileService에 getProfile(userId) 메서드를 이용해 더 간결하게 변경!
     ***/
    //커스터머 프로필 존재 여부 확인
    CustomerProfile customerProfile = customerProfileService.getProfile(userId);

    //리뷰 생성 및 저장
    Review review = Review.from(request, customerProfile);
    reviewRepository.save(review);
    log.debug("리뷰 저장 완료 - reviewId: {}", review.getId());

    //리뷰 사진 생성 및 저장
    List<ReviewPhoto> reviewPhotos = Arrays.stream(request.photoUrls())
        .map(url -> ReviewPhoto.builder()
            .photoUrl(url)
            .review(review)
            .build())
        .toList();
    reviewPhotoRepository.saveAll(reviewPhotos);
    log.debug("리뷰 사진 {}개 저장 완료 - reviewId : {}", reviewPhotos.size(), review.getId());

    //사진 URL 리스트 반환
    List<String> reviewPhotoUrls = getReviewPhotoUrlList(review);
    log.info("리뷰 생성 성공 - reviewId: {}, userId: {}", review.getId(), userId);

    // 알림용 Redis 저장
    NotificationType type = NotificationType.NEW_REVIEW;
    String reviewNotificationKey = "notifications:hourly:profile:" + review.getTargetId();

    redisTemplate.opsForHash().increment(reviewNotificationKey, type, 1);

    // 2시간 후 자동 삭제
    redisTemplate.expire(reviewNotificationKey, 2, TimeUnit.HOURS);

    Map<String, Object> data = Map.of("reviewId", review.getId());

    // 알림용 Notification 객체 저장
    Notification notification = new Notification();
    notification.setRecipientId(review.getTargetId());
    notification.setType(NotificationType.NEW_REVIEW);
    notification.setMessage("새 리뷰가 도착했습니다.");
    notification.setData(data.toString());

    notificationRepository.save(notification);

    return ReviewCreateResponse.from(review, reviewPhotoUrls, customerProfile);
  }

  /* 리뷰 삭제 */
  public void deleteReview(Long userId, Long reviewId) {
    log.info("리뷰 삭제 요청 - userId: {}, reviewId: {}", userId, reviewId);
    Review review = findById(reviewId);

    if (verifyReviewAuth(review, userId)) {
      reviewRepository.delete(review);
      log.info("리뷰 삭제 성공 - reviewId: {}", reviewId);
    } else {
      log.warn("리뷰 삭제 권한 없음 - userId: {}, reviewId: {}", userId, reviewId);
      throw new CustomException(ErrorCode.REVIEW_NO_PERMISSION);
    }
  }

  /* 리뷰 수정 */
  public ReviewResponse updateReview(ReviewUpdateRequest request, Long userId, Long reviewId) {
    log.info("리뷰 수정 요청 - userId: {}, request: {}, reviewId: {}", userId, request, reviewId);

    //유저 존재 여부 확인
    User user = userService.findById(userId);

    Review review = findById(reviewId);

    //유저 권한 체크
    if (!verifyReviewAuth(review, userId)) {
      log.warn("리뷰 수정 권한 없음 - userId: {}, reviewId: {}", userId, reviewId);
      throw new CustomException(ErrorCode.REVIEW_NO_PERMISSION);
    }

    //리뷰 업데이트
    review.update(request);
    review.updateReviewPhoto(request.photoUrls());
    reviewRepository.save(review);
    log.info("리뷰 수정 성공 - userId: {}, reviewId: {}", userId, reviewId);

    String reviewLikeKey = "review:likes:" + reviewId;
    Long likeCount = redisTemplate.opsForSet().size(reviewLikeKey);

    return ReviewResponse.from(review, getReviewPhotoUrlList(review), likeCount);
  }

  /* 단일 리뷰 조회 */
  public ReviewResponse getReview(Long reviewId) {
    log.info("리뷰 조회 요청 - reviewId: {}", reviewId);
    Review review = findById(reviewId);

    List<String> reviewPhotoUrls = getReviewPhotoUrlList(review);
    String reviewLikeKey = "review:likes:" + reviewId;
    Long likeCount = redisTemplate.opsForSet().size(reviewLikeKey);

    log.info("리뷰 조회 성공 - reviewId: {}", reviewId);
    return ReviewResponse.from(review, reviewPhotoUrls, likeCount);
  }

  /* 리뷰 리스트 조회 */
  public ReviewRatingAndListResponseDto getMyReviews(Long userId, Profile profile,
      MyReviewSortType sort,
      String cursor, Integer size) {
    log.info("내 리뷰 리스트 조회 요청 - userId: {}, sort: {}, cursor: {}, size: {}", userId, sort, cursor,
        size);

    ProfileType profileType = profile.getType();
    Long profileId = profile.getId();

    String[] decodedCursor = (String[]) CursorUtil.decode(cursor);

    //실제 조회
    List<ReviewResponse> reviewList = getReviewsByProfile(profileType, profileId, sort,
        decodedCursor,
        size);

    boolean hasNext = reviewList.size() > size;

    //클라이언트 전달값
    List<ReviewResponse> reviewResponse = hasNext ? reviewList.subList(0, size) : reviewList;

    String nextPageToken = null;
    if (!reviewList.isEmpty()) {
      ReviewResponse lastReview = reviewList.get(reviewResponse.size() - 1);

      String cursorValue = switch (sort) {
        case LATEST, OLDEST -> lastReview.createdAt().toString();
        case RATING_DESC, RATING_ASC -> String.valueOf(lastReview.rating());
      };

      // nextPageToken 구조: [정렬 기준 값, reviewId]
      // 예: LATEST → ["2025-09-22T08:00:00", 123]
      //      RATING_DESC → ["5", 123]
      nextPageToken = CursorUtil.encode(cursorValue, lastReview.id());

    }

    CursorPageResponse<ReviewResponse> result = new CursorPageResponse<>(reviewResponse,
        nextPageToken, hasNext);

    Double avgRating = switch (profileType) {
      case CUSTOMER -> getCustomerReviewRating(userId);
      case SELLER -> getSellerOrRiderReviewRating(userId, ReviewTargetType.STORE);
      case RIDER -> getSellerOrRiderReviewRating(userId, ReviewTargetType.RIDER);
    };

    log.info("리뷰 리스트 조회 성공 - userId: {}, resultCount: {}", userId, reviewResponse.size());
    log.debug("nextPageToken: {}", nextPageToken);
    return new ReviewRatingAndListResponseDto(avgRating, result);
  }

  /* 상점 리뷰 리스트 조회 */
  public ReviewRatingAndListResponseDto getStoreReviews(Long storeId, StoreReviewSortType sort,
      String cursor, int size) {
    log.info("상점 리뷰 리스트 조회 요청 - storeId: {}, sort: {}, cursor: {}, size: {}", storeId, sort, cursor,
        size);

    String[] decodedCursor = (String[]) CursorUtil.decode(cursor);

    //실제 조회
    List<Review> reviews = reviewRepository.getStoreReviews(storeId, sort,
        decodedCursor,
        size);

    List<ReviewResponse> reviewList = new ArrayList<>();

    for (Review review : reviews) {
      Long likeCount = redisTemplate.opsForSet().size("review:likes:" + review.getId());
      reviewList.add(ReviewResponse.from(review, getReviewPhotoUrlList(review), likeCount));
    }

    boolean hasNext = reviewList.size() > size;

    //클라이언트 전달값
    List<ReviewResponse> reviewResponses = hasNext ? reviewList.subList(0, size) : reviewList;

    String nextPageToken = null;
    if (!reviewList.isEmpty()) {
      ReviewResponse lastReview = reviewList.get(reviewResponses.size() - 1);

      String cursorValue = switch (sort) {
        case LATEST, OLDEST -> lastReview.createdAt().toString();
        case RATING_DESC, RATING_ASC -> String.valueOf(lastReview.rating());
        case LIKED_DESC -> String.valueOf(lastReview.likeCount());
      };

      nextPageToken = CursorUtil.encode(cursorValue, lastReview.id());

    }

    CursorPageResponse<ReviewResponse> result = new CursorPageResponse<>(reviewResponses,
        nextPageToken, hasNext);
    Double avgRating = getStoreReviewRating(storeId);

    log.info("상점 리뷰 리스트 조회 성공 - storeId: {}, resultCount: {}", storeId, reviewResponses.size());
    log.debug("nextPageToken: {}", nextPageToken);
    return new ReviewRatingAndListResponseDto(avgRating, result);
  }

  /* 리뷰 좋아요 추가 */
  public ReviewLikeResponse likeReview(Long reviewId, Long userId) {
    log.info("리뷰 좋아요 요청 - userId: {}, reviewId: {}", userId, reviewId);
    Review review = findById(reviewId);

    if (review.getTargetType() != ReviewTargetType.STORE) {
      log.warn("잘못된 리뷰 대상 - reviewId: {}, targetType: {}", reviewId, review.getTargetType());
      throw new CustomException(ErrorCode.REVIEW_INVALID_TARGET);
    }

    String reviewLikeKey = "review:likes:" + reviewId;
    String reviewSortedKey = "review:likes:store:" + review.getTargetId();

    Long likeCount = executeLikeAction(reviewLikeKey, reviewSortedKey, userId, reviewId,
        LikeAction.LIKE);

    if (likeCount == -1) {
      log.warn("이미 좋아요한 리뷰 - userId: {}, reviewId: {}", userId, reviewId);
      throw new CustomException(ErrorCode.REVIEW_ALREADY_LIKED);
    }

    log.info("리뷰 좋아요 성공 - userId: {}, reviewId: {}, likeCount: {}", userId, reviewId, likeCount);
    return new ReviewLikeResponse(reviewId, likeCount, true);
  }

  /* 리뷰 좋아요 취소 */
  public ReviewLikeResponse unlikeReview(Long reviewId, Long userId) {
    log.info("리뷰 좋아요 취소 요청 - userId: {}, reviewId: {}", userId, reviewId);
    Review review = findById(reviewId);

    if (review.getTargetType() != ReviewTargetType.STORE) {
      log.warn("잘못된 리뷰 대상 - reviewId: {}, targetType: {}", reviewId, review.getTargetType());
      throw new CustomException(ErrorCode.REVIEW_INVALID_TARGET);
    }

    // Redis key 설정
    String reviewLikeKey = "review:likes:" + reviewId;
    String reviewSortedKey = "review:likes:store:" + review.getTargetId();

    Long likeCount = executeLikeAction(reviewLikeKey, reviewSortedKey, userId, reviewId,
        LikeAction.UNLIKE);

    // 좋아요가 없는 경우 예외 처리
    if (likeCount == -1) {
      log.warn("좋아요를 누르지 않은 리뷰 - userId: {}, reviewId: {}", userId, reviewId);
      throw new CustomException(ErrorCode.REVIEW_NOT_LIKED);
    }

    log.info("리뷰 좋아요 취소 성공 - userId: {}, reviewId: {}, likeCount: {}", userId, reviewId, likeCount);
    return new ReviewLikeResponse(reviewId, likeCount, false);
  }

  /* 리뷰 좋아요 수 조회 */
  public ReviewLikeResponse getReviewLikeCount(Long reviewId, Long userId) {
    log.info("리뷰 좋아요 수 조회 요청 - reviewId: {}", reviewId);
    Review review = findById(reviewId);

    if (review.getTargetType() != ReviewTargetType.STORE) {
      log.warn("잘못된 리뷰 대상 - reviewId: {}, targetType: {}", reviewId, review.getTargetType());
      throw new CustomException(ErrorCode.REVIEW_INVALID_TARGET);
    }

    String reviewLikeKey = "review:likes:" + reviewId;

    Long likeCount = redisTemplate.opsForSet().size(reviewLikeKey);
    Boolean likedByMe = redisTemplate.opsForSet().isMember(reviewLikeKey, userId);

    log.info("리뷰 좋아요 수 조회 성공 - reviewId: {}, likeCount: {}", reviewId, likeCount);
    return new ReviewLikeResponse(reviewId, likeCount, likedByMe);
  }


  //=============================편의 메서드====================================
  /* 리뷰 사진 URL 리스트 반환 */
  @Transactional(readOnly = true)
  public List<String> getReviewPhotoUrlList(Review review) {
    return review.getReviewPhotos().stream()
        .map(ReviewPhoto::getPhotoUrl)
        .toList();
  }

  /* 리뷰 권한 확인 (작성자 확인) */
  @Transactional(readOnly = true)
  public boolean verifyReviewAuth(Review review, Long userId) {
    CustomerProfile customerProfile = customerProfileService.getProfile(
        userId); // daran2 - 이렇게 userId로 프로필 조회할 수 있도록 변경했습니당

    return review.getCustomerProfile().getId().equals(customerProfile.getId());
  }

  /* 프로필에 따라 내 리뷰 조회하기 */
  public List<ReviewResponse> getReviewsByProfile(ProfileType profileType, Long profileId,
      MyReviewSortType sort, String[] cursor, int size) {
    List<Review> reviews = reviewRepository.findReviewsByProfile(
        profileId,    // 변경된 부분
        profileType, // ProfileType 전달
        sort,        // ReviewSortType 전달
        cursor,
        size
    );

    return reviews.stream()
        .map(review -> {
          List<String> photoUrls = getReviewPhotoUrlList(review);
          String reviewLikeKey = "review:likes:" + review.getId();
          Long likeCount = redisTemplate.opsForSet().size(reviewLikeKey);
          return ReviewResponse.from(review, photoUrls, likeCount);
        })
        .toList();
  }

  public Review findById(Long reviewId) {
    return reviewRepository.findById(reviewId)
        .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
  }

  /* 리뷰 좋아요 순 정렬 리스트 반환 */
  public List<ReviewLikeResponse> getReviewsSortedByLikes(Long storeId, Long userId) {
    //상점 내 리뷰 조회 시 좋아요 순 정렬을 가능하게 해주는 메서드. 리뷰 api 내에서는 사용되지 않습니다.
    String reviewSortedKey = "review:likes:store:" + storeId;

    //모든 값 조회
    Set<TypedTuple<Object>> allReviews = redisTemplate.opsForZSet()
        .reverseRangeWithScores(reviewSortedKey, 0, -1);

    List<ReviewLikeResponse> list = new ArrayList<>();

    if (allReviews != null) {
      for (TypedTuple<Object> tuple : allReviews) {
        if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
          continue;
        }

        Long reviewId = Long.valueOf(tuple.getValue().toString());
        Long score = tuple.getScore().longValue();

        Boolean likedByMe = (userId != null) && Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember("review:likes:" + reviewId, userId));

        list.add(new ReviewLikeResponse(reviewId, score, likedByMe));
      }
    }

    return list;
  }

  /* 리뷰 Like, Unlike 로직 */
  private Long executeLikeAction(String reviewLikeKey, String reviewSortedKey, Long userId,
      Long reviewId, LikeAction action) {
    String luaScript = """
            local memberExists = redis.call('SISMEMBER', KEYS[1], ARGV[2])
        
                if (ARGV[1] == 'LIKE' and memberExists == 1) or (ARGV[1] == 'UNLIKE' and memberExists == 0) then
                    return -1
                end
        
                if ARGV[1] == 'LIKE' then
                    redis.call('SADD', KEYS[1], ARGV[2])
                else
                    redis.call('SREM', KEYS[1], ARGV[2])
                end
        
                redis.call('ZINCRBY', KEYS[2], tonumber(ARGV[3]), ARGV[4])
                return redis.call('SCARD', KEYS[1])
        """;

    return redisTemplate.execute((RedisCallback<Long>) connection ->
        connection.eval(
            luaScript.getBytes(),
            ReturnType.INTEGER,
            2,
            reviewLikeKey.getBytes(),
            reviewSortedKey.getBytes(),
            action.name().getBytes(),   // ARGV[1] : LIKE / UNLIKE
            userId.toString().getBytes(), // ARGV[2] : userId
            String.valueOf(action.getIncrement()).getBytes(), // ARGV[3] : +1/-1
            reviewId.toString().getBytes() // ARGV[4] : ZSet member
        )
    );
  }

  /* 별점 평균값 조회 메서드 - 판매자, 배달원 */
  private Double getSellerOrRiderReviewRating(
      Long userId, ReviewTargetType type) {
    Double avg = reviewRepository.findAvgRatingByTargetIdAndTargetType(userId, type);
    if (avg == null) {
      return 0.0;
    }
    return Math.round(avg * 100.0) / 100.0;
  }

  /* 별점 평균값 조회 메서드 - 소비자 */
  private Double getCustomerReviewRating(
      Long userId) {
    Double avg = reviewRepository.findAvgRatingByCustomerProfileId(userId);
    if (avg == null) {
      return 0.0;
    }
    return Math.round(avg * 100.0) / 100.0;
  }

  /* 별점 평균값 조회 메서드 - 상점 */
  private Double getStoreReviewRating(Long storeId) {
    Double avg = reviewRepository.findAvgRatingByStoreId(storeId);
    if (avg == null) {
      return 0.0;
    }
    return Math.round(avg * 100.0) / 100.0;
  }
}
