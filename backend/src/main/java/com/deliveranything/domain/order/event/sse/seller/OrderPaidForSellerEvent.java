package com.deliveranything.domain.order.event.sse.seller;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record OrderPaidForSellerEvent(
    Long orderId,
    List<OrderItemInfo> orderItems,
    Long sellerId,
    String address,
    String storeNote,
    Long totalPrice
) {

  public static OrderPaidForSellerEvent fromOrder(Order order) {
    return new OrderPaidForSellerEvent(
        order.getId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList(),
        order.getStore().getSellerProfileId(),
        order.getAddress(),
        order.getStoreNote(),
        order.getTotalPrice()
    );
  }
}
