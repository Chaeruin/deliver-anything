package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.Publisher;

public record OrderRejectedEvent(
    Long orderId,
    String merchantUid,
    String cancelReason,
    Publisher publisher
) {

  public static OrderRejectedEvent from(Order order, String cancelReason, Publisher publisher) {
    return new OrderRejectedEvent(
        order.getId(),
        order.getMerchantId(),
        cancelReason,
        publisher
    );
  }
}
