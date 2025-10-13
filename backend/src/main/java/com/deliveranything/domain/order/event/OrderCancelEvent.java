package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.Publisher;

public record OrderCancelEvent(
    Long orderId,
    String merchantUid,
    String cancelReason,
    Publisher publisher
) {

  public static OrderCancelEvent from(Order order, String cancelReason, Publisher publisher) {
    return new OrderCancelEvent(
        order.getId(),
        order.getMerchantId(),
        cancelReason,
        publisher
    );
  }
}
