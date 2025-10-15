package com.deliveranything.domain.order.service;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.order.event.OrderCancelEvent;
import com.deliveranything.domain.order.event.OrderPaymentRequestedEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentOrderService {

  private final ApplicationEventPublisher eventPublisher;
  private final OrderRepository orderRepository;

  @Transactional
  public void payOrder(String merchantUid, String paymentKey) {
    log.info("고객의 결제 시도 - 주문 번호: {} & PG 사 결제 키: {}", merchantUid, paymentKey);

    Order order = getOrderByMerchantId(merchantUid);
    order.isPayable();

    eventPublisher.publishEvent(
        OrderPaymentRequestedEvent.fromOrderAndPaymentKey(order, paymentKey));
  }

  @Transactional
  public void cancelOrder(Long orderId, String cancelReason) {
    Order order = getOrderById(orderId);
    order.cancellationRequest(cancelReason);

    eventPublisher.publishEvent(OrderCancelEvent.from(order, cancelReason, Publisher.CUSTOMER));
  }

  private Order getOrderByMerchantId(String merchantId) {
    return orderRepository.findByMerchantId(merchantId)
        .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
  }

  private Order getOrderById(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
  }
}
