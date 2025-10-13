//package com.deliveranything.domain.review.controller;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import com.deliveranything.domain.review.dto.ReviewCreateRequest;
//import com.deliveranything.domain.review.dto.ReviewCreateResponse;
//import com.deliveranything.domain.review.dto.ReviewResponse;
//import com.deliveranything.domain.review.dto.ReviewUpdateRequest;
//import com.deliveranything.domain.review.enums.MyReviewSortType;
//import com.deliveranything.domain.review.enums.ReviewTargetType;
//import com.deliveranything.domain.review.repository.ReviewRepository;
//import com.deliveranything.domain.review.service.ReviewService;
//import com.deliveranything.domain.user.entity.User;
//import com.deliveranything.domain.user.entity.profile.CustomerProfile;
//import com.deliveranything.domain.user.entity.profile.Profile;
//import com.deliveranything.domain.user.enums.ProfileType;
//import com.deliveranything.domain.user.repository.UserRepository;
//import com.deliveranything.global.common.CursorPageResponse;
//import com.deliveranything.global.exception.CustomException;
//import jakarta.persistence.EntityManager;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.transaction.annotation.Transactional;
//
//@SpringBootTest
//@EnableJpaAuditing
//@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//public class ReviewControllerTest {
//
//  @Autowired
//  private ReviewService reviewService;
//
//  @Autowired
//  private UserRepository userRepository;
//
//  @Autowired
//  private ReviewRepository reviewRepository;
//
//  @Autowired
//  private EntityManager em;
//
//  @Test
//  @DisplayName("리뷰 등록 - 정상")
//  public void createReview() {
//    User user = User.builder()
//        .email("test@example.com")
//        .name("testUser")
//        .password("testPassword")
//        .phoneNumber("testPhoneNumber")
//        .socialProvider(null)
//        .build();
//    Profile profile = Profile.builder()
//        .type(ProfileType.CUSTOMER)
//        .user(user)
//        .build();
//    CustomerProfile customerProfile = CustomerProfile.builder()
//        .profile(profile)
//        .customerPhoneNumber("")
//        .nickname("testUser")
//        .build();
//    userRepository.save(user);
//
//    ReviewCreateRequest request = ReviewFactory.createReviews(1).getFirst();
//    ReviewCreateResponse response = reviewService.createReview(request, user.getId());
//
//    assertNotNull(response.id());
//    assertEquals(1, response.rating());
//    assertEquals("test comment1", response.comment());
//  }
//
//  @Test
//  @DisplayName("리뷰 삭제 - 정상")
//  public void deleteReview() {
//    User user = User.builder()
//        .email("test@example.com")
//        .name("testUser")
//        .password("testPassword")
//        .phoneNumber("testPhoneNumber")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(user)
//        .nickname("testUser")
//        .build();
//    user.setCustomerProfile(profile);
//    userRepository.save(user);
//
//    ReviewCreateRequest request = ReviewFactory.createReviews(1).get(0);
//    ReviewCreateResponse response = reviewService.createReview(request, user.getId());
//
//    reviewService.deleteReview(user.getId(), response.id());
//
//    boolean exists = reviewRepository.findById(response.id()).isPresent();
//    assertFalse(exists, "리뷰가 삭제되어야 합니다");
//  }
//
//  @Test
//  @DisplayName("리뷰 삭제 - 존재하지 않는 리뷰")
//  public void deleteReview_nonExistentReview_throwsException() {
//    CustomException exception = assertThrows(CustomException.class, () -> {
//      reviewService.deleteReview(1L, 1L);
//    });
//
//    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
//    assertEquals("REVIEW-404", exception.getCode());
//    assertEquals("리뷰를 찾을 수 없습니다.", exception.getMessage());
//  }
//
//  @Test
//  @DisplayName("리뷰 삭제 - 권한 없는 유저의 요청")
//  public void deleteReview_userWithoutPermission_throwsException() {
//    User owner = User.builder()
//        .email("owner@example.com")
//        .name("ownerUser")
//        .password("ownerPassword")
//        .phoneNumber("010-0000-0000")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(owner)
//        .nickname("testUser")
//        .build();
//    owner.setCustomerProfile(profile);
//    userRepository.save(owner);
//
//    ReviewCreateRequest request = ReviewFactory.createReviews(1).getFirst();
//    ReviewCreateResponse createdReview = reviewService.createReview(request, owner.getId());
//
//    User otherUser = User.builder()
//        .email("other@example.com")
//        .name("otherUser")
//        .password("otherPassword")
//        .phoneNumber("010-1111-1111")
//        .socialProvider(null)
//        .build();
//    CustomerProfile otherProfile = CustomerProfile.builder()
//        .user(otherUser)
//        .nickname("testUser2")
//        .build();
//    otherUser.setCustomerProfile(otherProfile);
//    userRepository.save(otherUser);
//
//    CustomException exception = assertThrows(CustomException.class, () -> {
//      reviewService.deleteReview(otherUser.getId(), createdReview.id());
//    });
//
//    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
//    assertEquals("REVIEW-403", exception.getCode());
//    assertEquals("리뷰를 관리할 권한이 없습니다.", exception.getMessage());
//  }
//
//  @Test
//  @DisplayName("리뷰 조회 - 존재하지 않는 리뷰")
//  public void getReview_nonExistentReview_throwsException() {
//    CustomException exception = assertThrows(CustomException.class, () -> {
//      reviewService.getReview(1L);
//    });
//
//    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
//    assertEquals("REVIEW-404", exception.getCode());
//    assertEquals("리뷰를 찾을 수 없습니다.", exception.getMessage());
//  }
//
//  @Test
//  @DisplayName("리뷰 단건 조회 - 정상")
//  public void getReview() {
//    User user = User.builder()
//        .email("owner@example.com")
//        .name("ownerUser")
//        .password("ownerPassword")
//        .phoneNumber("010-0000-0000")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(user)
//        .nickname("testUser")
//        .build();
//    user.setCustomerProfile(profile);
//    userRepository.save(user);
//
//    ReviewCreateRequest reviewRq = ReviewFactory.createReviews(1).getFirst();
//    ReviewCreateResponse reviewRs = reviewService.createReview(reviewRq, user.getId());
//
//    ReviewResponse response = reviewService.getReview(reviewRs.id());
//
//    assertNotNull(response.id());
//    assertEquals(reviewRs.id(), response.id());
//    assertEquals(reviewRq.rating(), response.rating());
//    assertEquals(reviewRq.comment(), response.comment());
//
//    assertNotNull(response.createdAt());
//
//    LocalDateTime now = LocalDateTime.now();
//    assertTrue(!response.createdAt().isAfter(now.plusSeconds(1)) &&
//            !response.createdAt().isBefore(now.minusSeconds(1)),
//        "생성일이 현재 시간과 일치해야 합니다");
//  }
//
//  @Test
//  @DisplayName("리뷰 수정 - 정상")
//  public void updateReview_success() {
//    User user = User.builder()
//        .email("owner@example.com")
//        .name("ownerUser")
//        .password("ownerPassword")
//        .phoneNumber("010-0000-0000")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(user)
//        .nickname("testUser")
//        .build();
//    user.setCustomerProfile(profile);
//    userRepository.save(user);
//
//    ReviewCreateRequest createRequest = ReviewFactory.createReviews(1).getFirst();
//    ReviewCreateResponse createdReview = reviewService.createReview(createRequest, user.getId());
//
//    ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(
//        5,
//        "수정된 댓글",
//        new String[]{"new_photo1.jpg", "new_photo2.jpg"}
//    );
//
//    ReviewResponse updatedReview = reviewService.updateReview(
//        updateRequest,
//        user.getId(),
//        createdReview.id()
//    );
//
//    assertNotNull(updatedReview.id());
//    assertEquals(5, updatedReview.rating());
//    assertEquals("수정된 댓글", updatedReview.comment());
//    assertEquals(2, updatedReview.photoUrls().size());
//    assertTrue(updatedReview.photoUrls().contains("new_photo1.jpg"));
//    assertTrue(updatedReview.photoUrls().contains("new_photo2.jpg"));
//
//    assertNotNull(updatedReview.updatedAt());
//
//    LocalDateTime now = LocalDateTime.now();
//    assertTrue(!updatedReview.updatedAt().isAfter(now.plusSeconds(1)) &&
//            !updatedReview.updatedAt().isBefore(now.minusSeconds(1)),
//        "수정일이 현재 시간과 일치해야 합니다");
//  }
//
//  @Test
//  @DisplayName("리뷰 수정 - 권한 없는 유저의 요청")
//  public void updateReview_userWithoutPermission_throwsException() {
//    User owner = User.builder()
//        .email("owner@example.com")
//        .name("ownerUser")
//        .password("ownerPassword")
//        .phoneNumber("010-0000-0000")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(owner)
//        .nickname("testUser")
//        .build();
//    owner.setCustomerProfile(profile);
//    userRepository.save(owner);
//
//    ReviewCreateRequest request = ReviewFactory.createReviews(1).getFirst();
//    ReviewCreateResponse createdReview = reviewService.createReview(request, owner.getId());
//
//    User otherUser = User.builder()
//        .email("other@example.com")
//        .name("otherUser")
//        .password("otherPassword")
//        .phoneNumber("010-1111-1111")
//        .socialProvider(null)
//        .build();
//    CustomerProfile otherProfile = CustomerProfile.builder()
//        .user(otherUser)
//        .nickname("testUser2")
//        .build();
//    otherUser.setCustomerProfile(otherProfile);
//    userRepository.save(otherUser);
//
//    ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(
//        5,
//        "수정된 댓글",
//        new String[]{"new_photo1.jpg", "new_photo2.jpg"}
//    );
//
//    CustomException exception = assertThrows(CustomException.class, () -> {
//      reviewService.updateReview(updateRequest, otherUser.getId(), createdReview.id());
//    });
//
//    assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
//    assertEquals("REVIEW-403", exception.getCode());
//    assertEquals("리뷰를 관리할 권한이 없습니다.", exception.getMessage());
//  }
//
//  @Test
//  @DisplayName("리뷰 목록 조회 - 정상")
//  public void getReviewsByUser_success() {
//    User user = User.builder()
//        .email("reviewer@example.com")
//        .name("reviewerUser")
//        .password("reviewerPassword")
//        .phoneNumber("010-2222-3333")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(user)
//        .nickname("reviewerProfile")
//        .build();
//    user.setCustomerProfile(profile);
//    user.switchProfile(ProfileType.CUSTOMER);
//    userRepository.save(user);
//
//    List<ReviewCreateRequest> requests = ReviewFactory.createReviews(3);
//    for (ReviewCreateRequest rq : requests) {
//      reviewService.createReview(rq, user.getId());
//    }
//
//    CursorPageResponse<ReviewResponse> responses = reviewService.getReviews(user.getId(),
//        MyReviewSortType.RATING_ASC, null, 10);
//
//    assertEquals(3, responses.content().size(), "리뷰 개수가 일치해야 합니다");
//
//    for (int i = 0; i < responses.content().size(); i++) {
//      ReviewCreateRequest rq = requests.get(i);
//      ReviewResponse rs = responses.content().get(i);
//
//      assertNotNull(rs.id(), "리뷰 ID가 존재해야 합니다");
//      assertEquals(rq.rating(), rs.rating(), "리뷰 평점이 일치해야 합니다");
//      assertEquals(rq.comment(), rs.comment(), "리뷰 코멘트가 일치해야 합니다");
//    }
//  }
//
//  @Test
//  @DisplayName("리뷰 목록 조회 - 정렬 순서 검증")
//  public void getReviewsByUser_ordering_success() {
//    User user = User.builder()
//        .email("reviewer@example.com")
//        .name("reviewerUser")
//        .password("reviewerPassword")
//        .phoneNumber("010-2222-3333")
//        .socialProvider(null)
//        .build();
//    CustomerProfile profile = CustomerProfile.builder()
//        .user(user)
//        .nickname("reviewerProfile")
//        .build();
//    user.setCustomerProfile(profile);
//    user.switchProfile(ProfileType.CUSTOMER);
//    userRepository.save(user);
//
//    List<ReviewCreateRequest> requests = List.of(
//        new ReviewCreateRequest(5, "comment5", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(3, "comment3", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(4, "comment4", new String[]{}, ReviewTargetType.STORE, 1L)
//    );
//
//    for (ReviewCreateRequest rq : requests) {
//      reviewService.createReview(rq, user.getId());
//    }
//
//    CursorPageResponse<ReviewResponse> responses = reviewService.getReviews(user.getId(),
//        MyReviewSortType.RATING_ASC, null, 10);
//
//    assertEquals(3, responses.content().size(), "리뷰 개수가 일치해야 합니다");
//
//    List<Integer> ratings = responses.content().stream()
//        .map(ReviewResponse::rating)
//        .toList();
//
//    List<Integer> sortedRatings = ratings.stream().sorted().toList();
//
//    assertEquals(sortedRatings, ratings, "리뷰가 평점 오름차순으로 정렬되어야 합니다");
//
//    for (int i = 0; i < responses.content().size(); i++) {
//      ReviewResponse rs = responses.content().get(i);
//      assertNotNull(rs.id(), "리뷰 ID가 존재해야 합니다");
//      assertNotNull(rs.comment(), "리뷰 코멘트가 존재해야 합니다");
//    }
//  }
//
//  @Test
//  @DisplayName("리뷰 목록 조회 - Seller 정렬 순서 검증")
//  public void getReviewsBySeller_ordering_success() {
//    User user = User.builder()
//        .email("seller@example.com")
//        .name("sellerUser")
//        .password("sellerPassword")
//        .phoneNumber("010-4444-5555")
//        .socialProvider(null)
//        .build();
//    CustomerProfile customerProfile = CustomerProfile.builder()
//        .user(user)
//        .nickname("reviewerProfile")
//        .build();
//    user.switchProfile(ProfileType.SELLER);
//    user.setCustomerProfile(customerProfile);
//    userRepository.save(user);
//    em.flush();
//    em.clear();
//
//    List<ReviewCreateRequest> requests = List.of(
//        new ReviewCreateRequest(5, "comment5", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(3, "comment3", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(4, "comment4", new String[]{}, ReviewTargetType.STORE, 1L)
//    );
//
//    for (ReviewCreateRequest rq : requests) {
//      reviewService.createReview(rq, user.getId());
//    }
//
//    CursorPageResponse<ReviewResponse> responses = reviewService.getReviews(user.getId(),
//        MyReviewSortType.RATING_ASC, null, 10);
//
//    assertEquals(3, responses.content().size());
//
//    List<Integer> ratings = responses.content().stream().map(ReviewResponse::rating).toList();
//    assertEquals(ratings.stream().sorted().toList(), ratings);
//
//    for (ReviewResponse rs : responses.content()) {
//      assertNotNull(rs.id());
//      assertNotNull(rs.comment());
//    }
//  }
//
//  @Test
//  @DisplayName("리뷰 목록 조회 - Seller 최신순 정렬 검증")
//  public void getReviewsBySeller_latestOrder_success() {
//    User user = User.builder()
//        .email("seller@example.com")
//        .name("sellerUser")
//        .password("sellerPassword")
//        .phoneNumber("010-4444-5555")
//        .socialProvider(null)
//        .build();
//    CustomerProfile customerProfile = CustomerProfile.builder()
//        .user(user)
//        .nickname("reviewerProfile")
//        .build();
//    user.switchProfile(ProfileType.SELLER);
//    user.setCustomerProfile(customerProfile);
//    userRepository.save(user);
//    em.flush();
//    em.clear();
//
//    List<ReviewCreateRequest> requests = List.of(
//        new ReviewCreateRequest(5, "comment5", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(3, "comment3", new String[]{}, ReviewTargetType.STORE, 1L),
//        new ReviewCreateRequest(4, "comment4", new String[]{}, ReviewTargetType.STORE, 1L)
//    );
//
//    for (ReviewCreateRequest rq : requests) {
//      reviewService.createReview(rq, user.getId());
//    }
//
//    CursorPageResponse<ReviewResponse> responses = reviewService.getReviews(
//        user.getId(),
//        MyReviewSortType.LATEST,
//        null,
//        10
//    );
//
//    assertEquals(3, responses.content().size());
//
//    List<Long> ids = responses.content().stream().map(ReviewResponse::id).toList();
//    List<Long> sortedIdsDesc = ids.stream()
//        .sorted(Comparator.reverseOrder())
//        .toList();
//    assertEquals(sortedIdsDesc, ids);
//
//    for (ReviewResponse rs : responses.content()) {
//      assertNotNull(rs.id());
//      assertNotNull(rs.comment());
//    }
//  }
//
//  @BeforeEach
//  public void cleanUp() {
//    reviewRepository.deleteAll();
//    reviewRepository.flush();
//
//    userRepository.deleteAll();
//    userRepository.flush();
//  }
//
//
//  public static class ReviewFactory {
//
//    //리뷰 생성용 factory class
//    public static List<ReviewCreateRequest> createReviews(int count) {
//      List<ReviewCreateRequest> reviewCreateRequests = new ArrayList<>();
//
//      for (int i = 1; i <= count; i++) {
//        ReviewCreateRequest request = new ReviewCreateRequest(
//            i % 5,
//            "test comment" + i,
//            new String[]{"testUrl" + i},
//            ReviewTargetType.STORE,
//            (long) i
//        );
//
//        reviewCreateRequests.add(request);
//      }
//
//      return reviewCreateRequests;
//    }
//  }
//}