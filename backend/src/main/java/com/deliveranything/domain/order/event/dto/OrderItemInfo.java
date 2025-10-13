package com.deliveranything.domain.order.event.dto;

import com.deliveranything.domain.order.entity.OrderItem;

public record OrderItemInfo(
    Long productId,
    String productName,
    Integer quantity,
    Integer price
) {

  public static OrderItemInfo fromOrderItem(OrderItem orderItem) {
    return new OrderItemInfo(
        orderItem.getProduct().getId(),
        orderItem.getProduct().getName(),
        orderItem.getQuantity(),
        orderItem.getPrice()
    );
  }
}
