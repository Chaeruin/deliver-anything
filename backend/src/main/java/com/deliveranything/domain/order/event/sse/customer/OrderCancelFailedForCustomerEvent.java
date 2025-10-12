package com.deliveranything.domain.order.event.sse.customer;

import com.deliveranything.domain.order.entity.Order;

public record OrderCancelFailedForCustomerEvent(Long customerId) {

  public static OrderCancelFailedForCustomerEvent fromOrder(Order order) {
    return new OrderCancelFailedForCustomerEvent(order.getCustomer().getId());
  }
}
