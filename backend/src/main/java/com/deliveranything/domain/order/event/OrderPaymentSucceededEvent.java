package com.deliveranything.domain.order.event;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record OrderPaymentSucceededEvent(
    Long orderId,
    Long storeId,
    List<OrderItemInfo> orderItems
) {

  public static OrderPaymentSucceededEvent fromOrder(Order order) {
    return new OrderPaymentSucceededEvent(
        order.getId(),
        order.getStore().getId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList()
    );
  }
}
