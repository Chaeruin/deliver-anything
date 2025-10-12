package com.deliveranything.domain.order.event.dto;

import com.deliveranything.domain.order.entity.OrderItem;

public record OrderItemInfo(
    Long productId,
    String productName,
    Long quantity,
    Long price
) {

  public static OrderItemInfo fromOrderItem(OrderItem orderItem) {
    return new OrderItemInfo(
        orderItem.getId(),
        orderItem.getProduct().getName(),
        orderItem.getQuantity(),
        orderItem.getPrice()
    );
  }
}
