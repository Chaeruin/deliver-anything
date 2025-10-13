package com.deliveranything.domain.product.stock.publisher;

import com.deliveranything.domain.product.stock.event.StockCommittedEvent;
import com.deliveranything.domain.product.stock.event.StockReleasedEvent;
import com.deliveranything.domain.product.stock.event.StockReplenishedEvent;
import com.deliveranything.domain.product.stock.event.StockReservedEvent;
import com.deliveranything.domain.product.stock.event.StockReservedFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StockEventPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockReservedEvent(StockReservedEvent event) {
    redisTemplate.convertAndSend("stock-reserved-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockReservedFailedEvent(StockReservedFailedEvent event) {
    redisTemplate.convertAndSend("stock-reserved-failed-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockCommittedEvent(StockCommittedEvent event) {
    redisTemplate.convertAndSend("stock-committed-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockReleasedEvent(StockReleasedEvent event) {
    redisTemplate.convertAndSend("stock-released-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleStockReplenishedEvent(StockReplenishedEvent event) {
    redisTemplate.convertAndSend("stock-replenished-event", event);
  }
}