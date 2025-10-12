package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record OrderCancelSucceededEvent(
    Long orderId,
    List<OrderItemInfo> orderItems
) {

  public static OrderCancelSucceededEvent fromOrder(Order order) {
    return new OrderCancelSucceededEvent(
        order.getId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList()
    );
  }
}
