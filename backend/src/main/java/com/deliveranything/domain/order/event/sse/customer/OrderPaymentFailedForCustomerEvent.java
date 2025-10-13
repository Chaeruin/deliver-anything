package com.deliveranything.domain.order.event.sse.customer;

import com.deliveranything.domain.order.entity.Order;

public record OrderPaymentFailedForCustomerEvent(Long customerId, Long orderId) {

  public static OrderPaymentFailedForCustomerEvent fromOrder(Order order) {
    return new OrderPaymentFailedForCustomerEvent(order.getCustomer().getId(), order.getId());
  }
}
