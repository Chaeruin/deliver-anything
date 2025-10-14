package com.deliveranything.domain.order.dto;

import com.deliveranything.domain.order.entity.Order;

public record OrderCreateResponse(
    Long orderId,
    String merchantId
) {

  public static OrderCreateResponse from(Order order) {
    return new OrderCreateResponse(order.getId(), order.getMerchantId());
  }
}
