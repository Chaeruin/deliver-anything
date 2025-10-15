package com.deliveranything.domain.review.repository;

import com.deliveranything.domain.review.entity.Review;
import com.deliveranything.domain.review.enums.MyReviewSortType;
import com.deliveranything.domain.review.enums.StoreReviewSortType;
import com.deliveranything.domain.user.profile.enums.ProfileType;
import java.util.List;

public interface ReviewRepositoryCustom {

  List<Review> findReviewsByProfile(Long profileId, ProfileType profileType,
      MyReviewSortType sort, String[] cursor, int pageSize);

  int updateLikeCount(Long reviewId, int likeCount);

  List<Review> getStoreReviews(Long storeId, StoreReviewSortType sort, String[] decodedCursor,
      int size);

  Double findAvgRatingByStoreId(Long storeId);
}
