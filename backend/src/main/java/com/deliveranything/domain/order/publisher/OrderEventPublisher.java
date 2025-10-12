package com.deliveranything.domain.order.publisher;

import com.deliveranything.domain.delivery.event.dto.OrderAssignedEvent;
import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import com.deliveranything.domain.order.event.OrderCancelEvent;
import com.deliveranything.domain.order.event.OrderCancelSucceededEvent;
import com.deliveranything.domain.order.event.OrderCompletedEvent;
import com.deliveranything.domain.order.event.OrderCreatedEvent;
import com.deliveranything.domain.order.event.OrderPaymentFailedEvent;
import com.deliveranything.domain.order.event.OrderPaymentRequestedEvent;
import com.deliveranything.domain.order.event.OrderPaymentSucceededEvent;
import com.deliveranything.domain.order.event.OrderRejectedEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCancelFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCanceledForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPaidForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPaymentFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPreparingForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderStatusChangedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCancelFailedForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCanceledForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPaidForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPreparingForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderStatusChangedForSellerEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCreatedEvent(OrderCreatedEvent event) {
    redisTemplate.convertAndSend("order-created-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaymentRequestedEvent(OrderPaymentRequestedEvent event) {
    redisTemplate.convertAndSend("order-payment-requested-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaymentSucceededEvent(OrderPaymentSucceededEvent event) {
    redisTemplate.convertAndSend("order-payment-succeeded-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaymentFailedEvent(OrderPaymentFailedEvent event) {
    redisTemplate.convertAndSend("order-payment-Succeeded-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderRejectedEvent(OrderRejectedEvent event) {
    redisTemplate.convertAndSend("order-rejected-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderAcceptedEvent(OrderAcceptedEvent event) {
    redisTemplate.convertAndSend("order-accepted-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderAssignedEvent(OrderAssignedEvent event) {
    redisTemplate.convertAndSend("order-assigned-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCompletedEvent(OrderCompletedEvent event) {
    redisTemplate.convertAndSend("order-completed-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCancelEvent(OrderCancelEvent event) {
    redisTemplate.convertAndSend("order-cancel-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCancelSucceededEvent(OrderCancelSucceededEvent event) {
    redisTemplate.convertAndSend("order-cancel-succeeded-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaidForCustomerEvent(OrderPaidForCustomerEvent event) {
    redisTemplate.convertAndSend("order-paid-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaidForSellerEvent(OrderPaidForSellerEvent event) {
    redisTemplate.convertAndSend("order-paid-for-seller-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPaymentFailedForCustomerEvent(OrderPaymentFailedForCustomerEvent event) {
    redisTemplate.convertAndSend("order-payment-failed-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCanceledForCustomerEvent(OrderCanceledForCustomerEvent event) {
    redisTemplate.convertAndSend("order-canceled-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCanceledForSellerEvent(OrderCanceledForSellerEvent event) {
    redisTemplate.convertAndSend("order-canceled-for-seller-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCancelFailedForCustomerEvent(OrderCancelFailedForCustomerEvent event) {
    redisTemplate.convertAndSend("order-cancel-failed-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderCancelFailedForSellerEvent(OrderCancelFailedForSellerEvent event) {
    redisTemplate.convertAndSend("order-cancel-failed-for-seller-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderStatusChangedForCustomerEvent(OrderStatusChangedForCustomerEvent event) {
    redisTemplate.convertAndSend("order-status-changed-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderStatusChangedForSellerEvent(OrderStatusChangedForSellerEvent event) {
    redisTemplate.convertAndSend("order-status-changed-for-seller-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPreparedForCustomerEvent(OrderPreparingForCustomerEvent event) {
    redisTemplate.convertAndSend("order-preparing-for-customer-event", event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOrderPreparedForSellerEvent(OrderPreparingForSellerEvent event) {
    redisTemplate.convertAndSend("order-preparing-for-seller-event", event);
  }
}
