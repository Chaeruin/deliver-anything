package com.deliveranything.domain.review.scheduler;

import com.deliveranything.domain.review.dto.ReviewLikeEvent;
import com.deliveranything.domain.review.dto.ReviewResponse;
import com.deliveranything.domain.review.repository.ReviewRepository;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class ReviewLikeSyncScheduler {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ReviewRepository reviewRepository;

  public ReviewLikeSyncScheduler(RedisTemplate<String, Object> redisTemplate,
      ReviewRepository reviewRepository) {
    this.redisTemplate = redisTemplate;
    this.reviewRepository = reviewRepository;
  }

  @Scheduled(fixedRate = 5 * 60 * 1000)
  @Transactional
  public void syncLikesToDb() {
    String reviewSortedKey = "review:likes";

    //모든 리뷰 ID와 score 가져오기
    Set<ZSetOperations.TypedTuple<Object>> tuples =
        redisTemplate.opsForZSet().rangeWithScores(reviewSortedKey, 0, -1);

    for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
      Long reviewId = (Long) tuple.getValue();
      Double score = tuple.getScore();

      if (reviewId == null || score == null) {
        log.warn("잘못된 리뷰 좋아요 데이터: value={}, score={}", reviewId, score);
        continue;
      }

      int likeCount = score.intValue();

      // DB 업데이트
      int updated = reviewRepository.updateLikeCount(reviewId, likeCount);

      if (updated > 0) {
        log.info("리뷰 {} 좋아요 수 {}로 DB 반영 완료", reviewId, likeCount);
      } else {
        log.warn("리뷰 {}를 DB에서 찾을 수 없어 업데이트 실패", reviewId);
      }
    }
  }
}
