package com.deliveranything.domain.order.event.sse.customer;

import com.deliveranything.domain.order.entity.Order;

public record OrderPaidForCustomerEvent(Long customerId, Long orderId) {

  public static OrderPaidForCustomerEvent fromOrder(Order order) {
    return new OrderPaidForCustomerEvent(order.getCustomer().getId(), order.getId());
  }
}
