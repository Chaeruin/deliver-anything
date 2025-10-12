package com.deliveranything.domain.order.event.sse.customer;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.time.LocalDateTime;
import java.util.List;

public record OrderPreparingForCustomerEvent(
    Long orderId,
    Long customerId,
    List<OrderItemInfo> orderItems,
    OrderStatus status,
    String merchantId,
    String storeName,
    String address,
    String riderNote,
    String storeNote,
    Long totalPrice,
    Long storePrice,
    Long deliveryPrice,
    LocalDateTime createdAt
) {

  public static OrderPreparingForCustomerEvent fromOrder(Order order) {
    return new OrderPreparingForCustomerEvent(
        order.getId(),
        order.getCustomer().getId(),
        order.getOrderItems().stream().map(OrderItemInfo::fromOrderItem).toList(),
        order.getStatus(),
        order.getMerchantId(),
        order.getStore().getName(),
        order.getAddress(),
        order.getRiderNote(),
        order.getStoreNote(),
        order.getTotalPrice(),
        order.getStorePrice(),
        order.getDeliveryPrice(),
        order.getCreatedAt()
    );
  }
}
