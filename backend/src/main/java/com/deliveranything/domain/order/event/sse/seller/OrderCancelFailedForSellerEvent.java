package com.deliveranything.domain.order.event.sse.seller;

import com.deliveranything.domain.order.entity.Order;

public record OrderCancelFailedForSellerEvent(Long sellerId, Long orderId) {

  public static OrderCancelFailedForSellerEvent fromOrder(Order order) {
    return new OrderCancelFailedForSellerEvent(order.getStore().getSellerProfileId(),
        order.getId());
  }
}
