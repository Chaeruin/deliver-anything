package com.deliveranything.domain.order.service;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.order.event.OrderCancelSucceededEvent;
import com.deliveranything.domain.order.event.OrderCompletedEvent;
import com.deliveranything.domain.order.event.OrderPaymentFailedEvent;
import com.deliveranything.domain.order.event.OrderPaymentSucceededEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCancelFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPreparingForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderStatusChangedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCancelFailedForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPreparingForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderStatusChangedForSellerEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderService {

  private final ApplicationEventPublisher eventPublisher;
  private final OrderRepository orderRepository;

  @Transactional
  public void processPaymentCompletion(String merchantUid) {
    Order order = getOrderWithStoreByMerchantId(merchantUid);
    eventPublisher.publishEvent(OrderPaymentSucceededEvent.fromOrder(order));

    // TODO: 후에 StockCommittedEvent 듣고 그때 주문 상태 수정한 후에 아래 부분들 새 메서드에서 보내야함.
//    order.updateStatus(OrderStatus.PENDING);
//    eventPublisher.publishEvent(OrderPaidForCustomerEvent.fromOrder(order));
//    eventPublisher.publishEvent(OrderPaidForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processOrderTransmitted(Long orderId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.PREPARING);

    eventPublisher.publishEvent(OrderPreparingForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderPreparingForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processPaymentFailure(String merchantUid) {
    Order order = getOrderByMerchantId(merchantUid);
    eventPublisher.publishEvent(OrderPaymentFailedEvent.fromOrder(order));

    // TODO: 후에 StockReleasedEvent 듣고 그때 주문 상태 수정한 후에 아래 부분들 새 메서드에서 보내야함.
//    order.updateStatus(OrderStatus.PAYMENT_FAILED);
//    eventPublisher.publishEvent(OrderPaymentFailedForCustomerEvent.fromOrder(order));
  }

  @Transactional
  public void processPaymentCancelSuccess(String merchantUid, Publisher publisher) {
    Order order = getOrderByMerchantId(merchantUid);

    if (publisher == Publisher.CUSTOMER) {
      order.updateStatus(OrderStatus.CANCELED);
    } else if (publisher == Publisher.STORE) {
      order.updateStatus(OrderStatus.REJECTED);
    }

    eventPublisher.publishEvent(OrderCancelSucceededEvent.fromOrder(order));
    // TODO: 후에 StockReplensishedEvent 듣고 그때 주문 상태 수정한 후에 아래 부분들 새 메서드에서 보내야함.
//    eventPublisher.publishEvent(OrderCanceledForCustomerEvent.fromOrder(order));
//    eventPublisher.publishEvent(OrderCanceledForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processPaymentCancelFailed(String merchantUid) {
    Order order = getOrderByMerchantId(merchantUid);
    order.updateStatus(OrderStatus.CANCEL_FAILED);

    eventPublisher.publishEvent(OrderCancelFailedForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderCancelFailedForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processDeliveryRiderAssigned(Long orderId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.RIDER_ASSIGNED);

    eventPublisher.publishEvent(OrderStatusChangedForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderStatusChangedForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processDeliveryPickedUp(Long orderId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.DELIVERING);

    eventPublisher.publishEvent(OrderStatusChangedForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderStatusChangedForSellerEvent.fromOrder(order));
  }

  @Transactional
  public void processDeliveryCompleted(Long orderId, Long riderId, Long sellerId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.COMPLETED);

    eventPublisher.publishEvent(OrderCompletedEvent.fromOrder(order, riderId, sellerId));
    eventPublisher.publishEvent(OrderStatusChangedForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderStatusChangedForSellerEvent.fromOrder(order));
  }

  private Order getOrderWithStoreByMerchantId(String merchantUid) {
    return orderRepository.findOrderWithStoreByMerchantId(merchantUid)
        .orElseThrow(() -> new CustomException(
            ErrorCode.ORDER_NOT_FOUND));
  }

  private Order getOrderByMerchantId(String merchantUid) {
    return orderRepository.findByMerchantId(merchantUid).orElseThrow(() -> new CustomException(
        ErrorCode.ORDER_NOT_FOUND));
  }

  private Order getOrderById(Long orderId) {
    return orderRepository.findById(orderId).orElseThrow(() -> new CustomException(
        ErrorCode.ORDER_NOT_FOUND));
  }
}