package com.deliveranything.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.auth.enums.SocialProvider;
import com.deliveranything.domain.notification.enums.NotificationType;
import com.deliveranything.domain.notification.repository.NotificationRepository;
import com.deliveranything.domain.review.dto.ReviewCreateRequest;
import com.deliveranything.domain.review.dto.ReviewCreateResponse;
import com.deliveranything.domain.review.dto.ReviewRatingAndListResponseDto;
import com.deliveranything.domain.review.dto.ReviewResponse;
import com.deliveranything.domain.review.dto.ReviewUpdateRequest;
import com.deliveranything.domain.review.entity.Review;
import com.deliveranything.domain.review.enums.MyReviewSortType;
import com.deliveranything.domain.review.enums.ReviewTargetType;
import com.deliveranything.domain.review.enums.StoreReviewSortType;
import com.deliveranything.domain.review.repository.ReviewPhotoRepository;
import com.deliveranything.domain.review.repository.ReviewRepository;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.entity.Profile;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.deliveranything.domain.user.profile.repository.CustomerProfileRepository;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.domain.user.user.entity.User;
import com.deliveranything.domain.user.user.service.UserService;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import io.jsonwebtoken.lang.Collections;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 테스트")
class ReviewServiceTest {

  @Mock
  private CustomerProfileService customerProfileService;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewPhotoRepository reviewPhotoRepository;

  @Mock
  private CustomerProfileRepository customerProfileRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private UserService userService;

  @Mock
  private HashOperations<String, Object, Object> hashOperations;

  @Mock
  private SetOperations<String, Object> setOperations;

  @Mock
  private StoreService storeService;

  @InjectMocks
  private ReviewService reviewService;

  private ReviewCreateRequest reviewCreateRequest;
  private Profile profile;
  private CustomerProfile customerProfile;
  private User user;

  @BeforeEach
  void setUp() throws Exception {
    reviewCreateRequest = new ReviewCreateRequest(
        5,
        "test comment",
        new String[] {"Url1", "Url2"},
        ReviewTargetType.STORE,
        2L
    );

    user = createUserForTest(1L);
    Profile profile = createProfileForTest(user, 11L);
    customerProfile = createCustomerProfileForTest(profile, user, 111L);
  }

  @Test
  @DisplayName("리뷰 생성 성공")
  void createReview_success() throws Exception {
    ReviewCreateResponse response = createReviewResponseForTest(11L);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.photoUrls()).containsExactlyInAnyOrder("Url1", "Url2");

    verify(customerProfileService).getProfile(11L);
    verify(reviewRepository).save(any());
    verify(reviewPhotoRepository).saveAll(any());
    verify(redisTemplate).opsForHash();
    verify(hashOperations)
        .increment("notifications:hourly:profile:" + reviewCreateRequest.targetId(),
            NotificationType.NEW_REVIEW, 1);
    verify(redisTemplate).expire("notifications:hourly:profile:" + reviewCreateRequest.targetId(),
        2, TimeUnit.HOURS);
    verify(notificationRepository).save(any());
  }

  @Test
  @DisplayName("리뷰 삭제 성공 - 작성자 본인")
  void deleteReview_success() throws Exception {
    ReviewCreateResponse response = createReviewResponseForTest(11L); // 작성자 userId
    Review review = createReviewForTest(0);

    when(reviewRepository.findById(response.id())).thenReturn(Optional.of(review));

    // 삭제 시도
    reviewService.deleteReview(11L, response.id());

    verify(reviewRepository).findById(response.id());
    verify(reviewRepository).delete(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 삭제 실패 - 작성자 아님")
  void deleteReview_fail() throws Exception {
    //리뷰 생성
    ReviewCreateResponse response = createReviewResponseForTest(11L);
    Review review = createReviewForTest(0);

    // 테스트용 작성자 ID
    User otherUser = createUserForTest(22L);
    Profile otherProfile = createProfileForTest(otherUser, 22L);
    CustomerProfile otherCustomerProfile = createCustomerProfileForTest(otherProfile, otherUser, 22L);

    when(reviewRepository.findById(response.id())).thenReturn(Optional.of(review));
    when(customerProfileService.getProfile(22L)).thenReturn(otherCustomerProfile);

    // 작성자가 아닌 경우 deleteReview 호출 시 예외 발생 확인
    assertThrows(CustomException.class, () -> reviewService.deleteReview(22L, response.id()));
  }

  @Test
  @DisplayName("리뷰 수정 성공 - 작성자 본인")
  void updateReview_success() throws Exception {
    //리뷰 생성
    ReviewCreateResponse response = createReviewResponseForTest(11L);
    Review review = createReviewForTest(0);

    //리뷰 수정 dto 생성
    ReviewUpdateRequest request = new ReviewUpdateRequest(3, "Bad", new String[] {"Url5", "Url6"});

    when(reviewRepository.findById(response.id())).thenReturn(Optional.of(review));
    when(userService.findById(11L)).thenReturn(user);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(5L);

    reviewService.updateReview(request, 11L, response.id());

    // Then: 검증
    // rating 및 comment 업데이트 확인
    assertThat(review.getRating()).isEqualTo(request.rating());
    assertThat(review.getComment()).isEqualTo(request.comment());

    // reviewPhotos 리스트 업데이트 확인 (clear 후 add)
    assertThat(review.getReviewPhotos()).hasSize(request.photoUrls().length);
    assertThat(review.getReviewPhotos().get(0).getPhotoUrl()).isEqualTo("Url5");
    assertThat(review.getReviewPhotos().get(1).getPhotoUrl()).isEqualTo("Url6");
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 작성자 아님")
  void updateReview_noPermission() throws Exception {
    //리뷰 생성
    ReviewCreateResponse response = createReviewResponseForTest(11L);
    Review review = createReviewForTest(0);

    ReviewUpdateRequest request = new ReviewUpdateRequest(3, "Bad", new String[] {"Url5", "Url6"});

    // 테스트용 작성자 ID
    User otherUser = createUserForTest(22L);
    Profile otherProfile = createProfileForTest(otherUser, 22L);
    CustomerProfile otherCustomerProfile = createCustomerProfileForTest(otherProfile, otherUser, 22L);

    when(reviewRepository.findById(response.id())).thenReturn(Optional.of(review));
    when(userService.findById(22L)).thenReturn(otherUser);
    when(customerProfileService.getProfile(22L)).thenReturn(otherCustomerProfile);

    // 작성자가 아닌 경우 updateReview 호출 시 예외 발생 확인
    assertThrows(CustomException.class, () -> reviewService.updateReview(request, 22L, response.id()));
  }

  @Test
  @DisplayName("단일 리뷰 조회 성공")
  void getReview_success() throws Exception {
    //리뷰 생성
    ReviewCreateResponse createResponse = createReviewResponseForTest(11L);
    Review review = createReviewForTest(0);

    when(reviewRepository.findById(createResponse.id())).thenReturn(Optional.of(review));
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(5L);

    ReviewResponse response = reviewService.getReview(createResponse.id());

    assertThat(response.id()).isEqualTo(createResponse.id());
    assertThat(response.rating()).isEqualTo(createResponse.rating());
    assertThat(response.comment()).isEqualTo(createResponse.comment());
    assertThat(response.likeCount()).isEqualTo(5L);
  }

  @Test
  @DisplayName("상점 리뷰 리스트 조회 성공 - LATEST, 첫 페이지")
  void getStoreReviews_success_latestFirstPage() throws Exception {
    // GIVEN
    Long STORE_ID = 1L;
    int SIZE = 5;
    StoreReviewSortType SORT_TYPE = StoreReviewSortType.LATEST;
    String CURSOR = null;

    List<Review> mockReviews = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 0; i < SIZE + 1; i++) {
      Review mockReview = mock(Review.class);

      when(mockReview.getCreatedAt()).thenReturn(now.minusHours(i));
      when(mockReview.getId()).thenReturn(100L + i);
      when(mockReview.getRating()).thenReturn(5);
      when(mockReview.getReviewPhotos()).thenReturn(Collections.emptyList());

      mockReviews.add(mockReview);
    }

    when(storeService.getStoreById(eq(STORE_ID))).thenReturn(mock(Store.class));
    when(reviewRepository.getStoreReviews(
        eq(STORE_ID),
        eq(SORT_TYPE),
        any(),
        eq(SIZE)
    )).thenReturn(mockReviews);

    SetOperations setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(10L);

    // WHEN
    ReviewRatingAndListResponseDto response = reviewService.getStoreReviews(
        STORE_ID, SORT_TYPE, CURSOR, SIZE);

    // THEN
    assertThat(response.reviews().content()).hasSize(SIZE);
    assertThat(response.reviews().hasNext()).isTrue();
    assertThat(response.reviews().nextPageToken()).isNotNull();
    assertThat(response.reviews().content().get(0).likeCount()).isEqualTo(10L);

    verify(reviewRepository).getStoreReviews(eq(STORE_ID), eq(SORT_TYPE), any(), eq(SIZE));
    verify(setOperations, times(SIZE+1)).size(anyString());
  }

  @Test
  @DisplayName("상점 리뷰 리스트 조회 성공 - RATING_DESC, 첫 페이지")
  void getStoreReviews_success_ratingDescFirstPage() throws Exception {
    Long STORE_ID = 1L;
    int SIZE = 5;
    StoreReviewSortType SORT_TYPE = StoreReviewSortType.RATING_DESC;
    String CURSOR = null;

    List<Review> mockReviews = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    int[] ratings = {5, 5, 4, 4, 3, 3};

    for (int i = 0; i < SIZE + 1; i++) {
      Review mockReview = mock(Review.class);

      when(mockReview.getRating()).thenReturn(ratings[i]);
      when(mockReview.getId()).thenReturn(100L + i);
      when(mockReview.getCreatedAt()).thenReturn(now.minusHours(i));
      when(mockReview.getReviewPhotos()).thenReturn(Collections.emptyList());

      mockReviews.add(mockReview);
    }

    when(storeService.getStoreById(eq(STORE_ID))).thenReturn(mock(Store.class));
    when(reviewRepository.getStoreReviews(
        eq(STORE_ID),
        eq(SORT_TYPE),
        any(),
        eq(SIZE)
    )).thenReturn(mockReviews);

    SetOperations setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(10L);

    ReviewRatingAndListResponseDto response = reviewService.getStoreReviews(
        STORE_ID, SORT_TYPE, CURSOR, SIZE);

    assertThat(response.reviews().content()).hasSize(SIZE);
    assertThat(response.reviews().hasNext()).isTrue();
    assertThat(response.reviews().nextPageToken()).isNotNull();
    assertThat(response.reviews().content().get(0).likeCount()).isEqualTo(10L);

    // 별점의 정렬 순서 확인 (5, 5, 4, 4, 3까지 총 5개)
    assertThat(response.reviews().content().get(0).rating()).isEqualTo(5);
    assertThat(response.reviews().content().get(SIZE - 1).rating()).isEqualTo(3);

    verify(reviewRepository).getStoreReviews(eq(STORE_ID), eq(SORT_TYPE), any(), eq(SIZE));
    verify(setOperations, times(SIZE + 1)).size(anyString());
  }

  @Test
  @DisplayName("상점 리뷰 리스트 조회 실패 - 존재하지 않는 상점")
  void getStoreReviews_fail_storeNotFound() {
    Long NON_EXISTENT_STORE_ID = 999L;
    int SIZE = 5;
    StoreReviewSortType SORT_TYPE = StoreReviewSortType.LATEST;
    String CURSOR = null;

    when(storeService.getStoreById(eq(NON_EXISTENT_STORE_ID))).thenReturn(null);

    assertThatThrownBy(() -> {
      reviewService.getStoreReviews(NON_EXISTENT_STORE_ID, SORT_TYPE, CURSOR, SIZE);
    })
        .isInstanceOf(CustomException.class)
        .satisfies(e -> {
          CustomException customException = (CustomException) e;
          assertThat(customException.getCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND.getCode());
        });

    verify(storeService).getStoreById(eq(NON_EXISTENT_STORE_ID));
    verify(reviewRepository, never()).getStoreReviews(any(), any(), any(), anyInt());
  }

  @Test
  @DisplayName("내 리뷰 리스트 조회 성공 - 소비자, LATEST, 첫 페이지")
  @MockitoSettings(strictness = Strictness.LENIENT)
  void getMyeReviews_success_latestFirstPage_consumer() throws Exception {
    Long PROFILE_ID = 1L;
    int SIZE = 5;
    String CURSOR = null;

    Profile profileMock = mock(Profile.class);
    when(profileMock.getType()).thenReturn(ProfileType.CUSTOMER);
    when(profileMock.getId()).thenReturn(PROFILE_ID);

    CustomerProfile customerProfile = mock(CustomerProfile.class);

    List<Review> mockReviews = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 0; i < SIZE + 1; i++) {
      Review mockReview = mock(Review.class);

      when(mockReview.getCreatedAt()).thenReturn(now.minusHours(i));
      when(mockReview.getId()).thenReturn(100L + i);
      when(mockReview.getRating()).thenReturn(5);
      when(mockReview.getReviewPhotos()).thenReturn(Collections.emptyList());
      when(mockReview.getCustomerProfile()).thenReturn(customerProfile);

      mockReviews.add(mockReview);
    }

    when(reviewRepository.findReviewsByProfile(
        eq(PROFILE_ID),
        eq(ProfileType.CUSTOMER),
        eq(MyReviewSortType.LATEST),
        any(),
        eq(SIZE)
    )).thenReturn(mockReviews);

    SetOperations setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(10L);

    ReviewRatingAndListResponseDto response = reviewService.getMyReviews(
        PROFILE_ID, profileMock, MyReviewSortType.LATEST, CURSOR, SIZE);

    assertThat(response.reviews().content()).hasSize(SIZE);
    assertThat(response.reviews().hasNext()).isTrue();
    assertThat(response.reviews().nextPageToken()).isNotNull();
    assertThat(response.reviews().content().get(0).likeCount()).isEqualTo(10L);

    verify(reviewRepository).findReviewsByProfile(eq(PROFILE_ID), eq(ProfileType.CUSTOMER), eq(MyReviewSortType.LATEST), any(), eq(SIZE));
    verify(setOperations, times(SIZE + 1)).size(anyString());
    verify(profileMock).getType();
    verify(profileMock).getId();
  }

  @Test
  @DisplayName("내 리뷰 리스트 조회 성공 - 판매원, LATEST, 첫 페이지")
  @MockitoSettings(strictness = Strictness.LENIENT)
  void getMyeReviews_success_latestFirstPage_seller() throws Exception {
    Long PROFILE_ID = 1L;
    int SIZE = 5;
    String CURSOR = null;

    Profile profileMock = mock(Profile.class);
    when(profileMock.getType()).thenReturn(ProfileType.SELLER);
    when(profileMock.getId()).thenReturn(PROFILE_ID);

    List<Review> mockReviews = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 0; i < SIZE + 1; i++) {
      Review mockReview = mock(Review.class);

      when(mockReview.getCreatedAt()).thenReturn(now.minusHours(i));
      when(mockReview.getId()).thenReturn(100L + i);
      when(mockReview.getRating()).thenReturn(5);
      when(mockReview.getReviewPhotos()).thenReturn(Collections.emptyList());
      when(mockReview.getTargetType()).thenReturn(ReviewTargetType.STORE);

      mockReviews.add(mockReview);
    }

    when(reviewRepository.findReviewsByProfile(
        eq(PROFILE_ID),
        eq(ProfileType.SELLER),
        eq(MyReviewSortType.LATEST),
        any(),
        eq(SIZE)
    )).thenReturn(mockReviews);

    SetOperations setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(10L);

    ReviewRatingAndListResponseDto response = reviewService.getMyReviews(
        PROFILE_ID, profileMock, MyReviewSortType.LATEST, CURSOR, SIZE);

    assertThat(response.reviews().content()).hasSize(SIZE);
    assertThat(response.reviews().hasNext()).isTrue();
    assertThat(response.reviews().nextPageToken()).isNotNull();
    assertThat(response.reviews().content().get(0).likeCount()).isEqualTo(10L);

    verify(reviewRepository).findReviewsByProfile(eq(PROFILE_ID), eq(ProfileType.SELLER), eq(MyReviewSortType.LATEST), any(), eq(SIZE));
    verify(setOperations, times(SIZE + 1)).size(anyString());
    verify(profileMock).getType();
    verify(profileMock).getId();
  }

  @Test
  @DisplayName("내 리뷰 리스트 조회 성공 - 배달원, LATEST, 첫 페이지")
  @MockitoSettings(strictness = Strictness.LENIENT)
  void getMyeReviews_success_latestFirstPage_rider() throws Exception {
    Long PROFILE_ID = 1L;
    int SIZE = 5;
    String CURSOR = null;

    Profile profileMock = mock(Profile.class);
    when(profileMock.getType()).thenReturn(ProfileType.RIDER);
    when(profileMock.getId()).thenReturn(PROFILE_ID);

    List<Review> mockReviews = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (int i = 0; i < SIZE + 1; i++) {
      Review mockReview = mock(Review.class);

      when(mockReview.getCreatedAt()).thenReturn(now.minusHours(i));
      when(mockReview.getId()).thenReturn(100L + i);
      when(mockReview.getRating()).thenReturn(5);
      when(mockReview.getReviewPhotos()).thenReturn(Collections.emptyList());
      when(mockReview.getTargetType()).thenReturn(ReviewTargetType.RIDER);

      mockReviews.add(mockReview);
    }

    when(reviewRepository.findReviewsByProfile(
        eq(PROFILE_ID),
        eq(ProfileType.RIDER),
        eq(MyReviewSortType.LATEST),
        any(),
        eq(SIZE)
    )).thenReturn(mockReviews);

    SetOperations setOperations = mock(SetOperations.class);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.size(anyString())).thenReturn(10L);

    ReviewRatingAndListResponseDto response = reviewService.getMyReviews(
        PROFILE_ID, profileMock, MyReviewSortType.LATEST, CURSOR, SIZE);

    assertThat(response.reviews().content()).hasSize(SIZE);
    assertThat(response.reviews().hasNext()).isTrue();
    assertThat(response.reviews().nextPageToken()).isNotNull();
    assertThat(response.reviews().content().get(0).likeCount()).isEqualTo(10L);

    verify(reviewRepository).findReviewsByProfile(eq(PROFILE_ID), eq(ProfileType.RIDER), eq(MyReviewSortType.LATEST), any(), eq(SIZE));
    verify(setOperations, times(SIZE + 1)).size(anyString());
    verify(profileMock).getType();
    verify(profileMock).getId();
  }


  //====================================================
  private ReviewCreateResponse createReviewResponseForTest(Long profileId) throws Exception {
    when(customerProfileService.getProfile(profileId)).thenReturn(customerProfile);
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    when(reviewRepository.save(any())).thenAnswer(invocation -> {
      Review r = invocation.getArgument(0);
      Field idField = Review.class.getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(r, 10L);
      return r;
    });
    return reviewService.createReview(reviewCreateRequest, profileId);
  }

  private Review createReviewForTest(int i) throws Exception {
    // review 객체 생성
    Review review = Review.from(reviewCreateRequest, customerProfile);

    review.updateReviewPhoto(reviewCreateRequest.photoUrls());

    // review ID 세팅
    Field reviewIdField = Review.class.getSuperclass().getDeclaredField("id");
    reviewIdField.setAccessible(true);
    reviewIdField.set(review, 10L + i);

      Field likeCountField = Review.class.getDeclaredField("likeCount");
      likeCountField.setAccessible(true);
      likeCountField.set(review, 5);

    return review;
  }

  private User createUserForTest(Long i) throws Exception {
    User user = User.builder()
        .email("1234")
        .socialProvider(SocialProvider.LOCAL)
        .build();

    // user ID 세팅
    Field cpIdField = User.class.getSuperclass().getDeclaredField("id");
    cpIdField.setAccessible(true);
    cpIdField.set(user, i); // 테스트용 ID

    return user;
  }

  private Profile createProfileForTest(User user, Long i) throws Exception {
    Profile profile = Profile.builder()
        .type(ProfileType.CUSTOMER)
        .user(user)
        .build();

    // profile ID 세팅
    Field cpIdField = Profile.class.getSuperclass().getDeclaredField("id");
    cpIdField.setAccessible(true);
    cpIdField.set(profile, i); // 테스트용 ID

    return profile;
  }

  private CustomerProfile createCustomerProfileForTest(Profile profile, User user, Long i) throws Exception {
    CustomerProfile customerProfile = CustomerProfile.builder()
        .customerPhoneNumber("010-1234-5678")
        .profileImageUrl("Url3")
        .nickname("testNickName")
        .profile(profile)
        .build();

    // customerProfile ID 세팅
    Field cpIdField = CustomerProfile.class.getDeclaredField("id");
    cpIdField.setAccessible(true);
    cpIdField.set(customerProfile, i); // 테스트용 ID

    user.setCurrentActiveProfile(profile);

    return customerProfile;
  }
  }
