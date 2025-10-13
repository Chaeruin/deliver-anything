package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;

public record OrderPaymentRequestedEvent(
    Long orderId,
    String paymentKey,
    String merchantUid,
    Long amount
) {

  public static OrderPaymentRequestedEvent fromOrderAndPaymentKey(Order order, String paymentKey) {
    return new OrderPaymentRequestedEvent(
        order.getId(),
        paymentKey,
        order.getMerchantId(),
        order.getTotalPrice()
    );
  }
}
