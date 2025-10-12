package com.deliveranything.domain.order.event.sse.seller;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record OrderPreparingForSellerEvent(
    Long orderId,
    Long sellerId,
    List<OrderItemInfo> orderItems,
    OrderStatus status,
    String address,
    String storeNote
) {

  public static OrderPreparingForSellerEvent fromOrder(Order order) {
    return new OrderPreparingForSellerEvent(
        order.getId(),
        order.getStore().getSellerProfileId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList(),
        order.getStatus(),
        order.getAddress(),
        order.getStoreNote()
    );
  }
}
