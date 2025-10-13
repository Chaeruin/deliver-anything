package com.deliveranything.domain.order.event.sse.customer;

import com.deliveranything.domain.order.entity.Order;

public record OrderCreatedForCustomerEvent(Long customerId, Long orderId, String merchantId) {

  public static OrderCreatedForCustomerEvent fromOrder(Order order) {
    return new OrderCreatedForCustomerEvent(
        order.getCustomer().getId(),
        order.getId(),
        order.getMerchantId()
    );
  }
}
