package com.deliveranything.domain.product.product.publisher;

import com.deliveranything.domain.product.product.event.ProductKeywordsChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleProductKeywordsChangedEvent(ProductKeywordsChangedEvent event) {
    redisTemplate.convertAndSend("product-keywords-events", event);
  }
}