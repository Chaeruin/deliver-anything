package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record OrderPaymentFailedEvent(
    Long orderId,
    List<OrderItemInfo> orderItems
) {

  public static OrderPaymentFailedEvent fromOrder(Order order) {
    return new OrderPaymentFailedEvent(
        order.getId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList()
    );
  }
}
