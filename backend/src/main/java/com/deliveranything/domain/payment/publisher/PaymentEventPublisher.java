package com.deliveranything.domain.payment.publisher;

import com.deliveranything.domain.payment.event.PaymentCancelFailedEvent;
import com.deliveranything.domain.payment.event.PaymentCancelSuccessEvent;
import com.deliveranything.domain.payment.event.PaymentFailedEvent;
import com.deliveranything.domain.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentCompletedEvent(PaymentSuccessEvent event) {
    redisTemplate.convertAndSend("payment-completed-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentFailedEvent(PaymentFailedEvent event) {
    redisTemplate.convertAndSend("payment-failed-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentCancelSuccessEvent(PaymentCancelSuccessEvent event) {
    redisTemplate.convertAndSend("payment-cancel-success-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePaymentCancelFailedEvent(PaymentCancelFailedEvent event) {
    redisTemplate.convertAndSend("payment-cancel-failed-event", event);
  }
}
