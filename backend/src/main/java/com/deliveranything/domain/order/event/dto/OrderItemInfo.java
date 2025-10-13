package com.deliveranything.domain.order.event.dto;

import com.deliveranything.domain.order.entity.OrderItem;

public record OrderItemInfo(
    Long productId,
    Integer quantity
) {

  public static OrderItemInfo fromOrderItem(OrderItem orderItem) {
    return new OrderItemInfo(
        orderItem.getProduct().getId(),
        orderItem.getQuantity()
    );
  }
}
