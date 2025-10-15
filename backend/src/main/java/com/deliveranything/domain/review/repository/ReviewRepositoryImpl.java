package com.deliveranything.domain.review.repository;

import com.deliveranything.domain.review.entity.QReview;
import com.deliveranything.domain.review.entity.Review;
import com.deliveranything.domain.review.enums.MyReviewSortType;
import com.deliveranything.domain.review.enums.ReviewTargetType;
import com.deliveranything.domain.review.enums.StoreReviewSortType;
import com.deliveranything.domain.store.store.entity.QStore;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Review> findReviewsByProfile(Long profileId,
      ProfileType profileType,
      MyReviewSortType sort,
      String[] cursor,
      int pageSize) {
    QReview review = QReview.review;

    // profileType에 따른 조건
    BooleanExpression profileCondition = switch (profileType) {
      case CUSTOMER -> review.customerProfile.id.eq(profileId);
      case SELLER -> review.targetId.eq(profileId)
          .and(review.targetType.eq(ReviewTargetType.STORE));
      case RIDER -> review.targetId.eq(profileId)
          .and(review.targetType.eq(ReviewTargetType.RIDER));
    };

    // 커서 조건
    BooleanExpression cursorCondition = null;
    if (cursor != null && cursor.length >= 2) {
      String cursorVal = cursor[0]; // createdAt 또는 rating 값
      Long cursorId = Long.parseLong(cursor[1]); // tie-breaker용 id

      cursorCondition = switch (sort) {
        case LATEST -> review.createdAt.lt(LocalDateTime.parse(cursorVal))
            .or(review.createdAt.eq(LocalDateTime.parse(cursorVal))
                .and(review.id.lt(cursorId)));

        case OLDEST -> review.createdAt.gt(LocalDateTime.parse(cursorVal))
            .or(review.createdAt.eq(LocalDateTime.parse(cursorVal))
                .and(review.id.gt(cursorId)));

        case RATING_DESC -> review.rating.lt(Integer.parseInt(cursorVal))
            .or(review.rating.eq(Integer.parseInt(cursorVal))
                .and(review.id.lt(cursorId)));

        case RATING_ASC -> review.rating.gt(Integer.parseInt(cursorVal))
            .or(review.rating.eq(Integer.parseInt(cursorVal))
                .and(review.id.gt(cursorId)));
      };
    }

    // 정렬 조건 (항상 tie-breaker로 id 포함)
    OrderSpecifier<?>[] orderSpecifiers = switch (sort) {
      case LATEST -> new OrderSpecifier[]{review.createdAt.desc(), review.id.desc()};
      case OLDEST -> new OrderSpecifier[]{review.createdAt.asc(), review.id.asc()};
      case RATING_DESC -> new OrderSpecifier[]{review.rating.desc(), review.id.desc()};
      case RATING_ASC -> new OrderSpecifier[]{review.rating.asc(), review.id.asc()};
    };

    return queryFactory
        .selectFrom(review)
        .where(profileCondition, cursorCondition)
        .orderBy(orderSpecifiers)
        .limit(pageSize + 1) // hasNext 판단용 +1
        .fetch();
  }


  @Override
  public int updateLikeCount(Long reviewId, int likeCount) {
    QReview review = QReview.review;

    long updatedRows = queryFactory.update(review)
        .set(review.likeCount, likeCount)
        .where(review.id.eq(reviewId))
        .execute();

    return (int) updatedRows;
  }

    @Override
  public List<Review> getStoreReviews(Long storeId, StoreReviewSortType sort,
      String[] decodedCursor, int pageSize) {
    QReview review = QReview.review;

    // 커서 값 초기화
    Long cursorId = null;
    Integer cursorValue = null;          // rating 또는 likeCount 기준
    LocalDateTime cursorCreatedAt = null;

    // decodedCursor가 존재하면 값 파싱
    if (decodedCursor != null && decodedCursor.length > 0) {
      switch (sort) {
        case LATEST:
        case OLDEST:
          cursorCreatedAt = LocalDateTime.parse(decodedCursor[0]);
          cursorId = Long.parseLong(decodedCursor[1]);
          break;
        case RATING_DESC:
        case RATING_ASC:
          cursorValue = Integer.parseInt(decodedCursor[0]);
          cursorId = Long.parseLong(decodedCursor[1]);
          break;
        case LIKED_DESC:
          cursorValue = Integer.parseInt(decodedCursor[0]);
          cursorId = Long.parseLong(decodedCursor[1]);
          break;
      }
    }

    // 커서 조건 생성
    BooleanExpression cursorCondition = null;
    if (cursorId != null) {
      cursorCondition = switch (sort) {
        case LATEST -> review.createdAt.lt(cursorCreatedAt)
            .or(review.createdAt.eq(cursorCreatedAt)
                .and(review.id.lt(cursorId)));
        case OLDEST -> review.createdAt.gt(cursorCreatedAt)
            .or(review.createdAt.eq(cursorCreatedAt)
                .and(review.id.gt(cursorId)));
        case RATING_DESC -> review.rating.lt(cursorValue)
            .or(review.rating.eq(cursorValue)
                .and(review.id.lt(cursorId)));
        case RATING_ASC -> review.rating.gt(cursorValue)
            .or(review.rating.eq(cursorValue)
                .and(review.id.gt(cursorId)));
        case LIKED_DESC -> review.likeCount.lt(cursorValue)
            .or(review.likeCount.eq(cursorValue)
                .and(review.id.lt(cursorId)));
      };
    }

    // 정렬 기준 생성 (복합 정렬)
    OrderSpecifier<?> primary = null, secondary = null;

    switch (sort) {
      case LATEST:
        primary = review.createdAt.desc();
        secondary = review.id.desc();
        break;
      case OLDEST:
        primary = review.createdAt.asc();
        secondary = review.id.asc();
        break;
      case RATING_DESC:
        primary = review.rating.desc();
        secondary = review.id.desc();
        break;
      case RATING_ASC:
        primary = review.rating.asc();
        secondary = review.id.asc();
        break;
      case LIKED_DESC:
        primary = review.likeCount.desc();
        secondary = review.id.desc();
        break;
    }

    // 실제 조회
    return queryFactory
        .selectFrom(review)
        .where(review.targetId.eq(storeId), cursorCondition)
        .orderBy(primary, secondary)
        .limit(pageSize + 1)
        .fetch();
  }

  @Override
  public Double findAvgRatingByStoreId(Long storeId) {
    QReview review = QReview.review;
    QStore store = QStore.store;

    return queryFactory
        .select(review.rating.avg())
        .from(review)
        .join(store).on(review.targetId.eq(store.sellerProfileId)) // 리뷰 targetId = 상점 소유자 프로필 ID
        .where(store.id.eq(storeId), review.targetType.eq(ReviewTargetType.STORE))
        .fetchOne();
  }
}
