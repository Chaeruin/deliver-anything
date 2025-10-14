package com.deliveranything.domain.order.service;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.order.event.OrderCancelSucceededEvent;
import com.deliveranything.domain.order.event.OrderCompletedEvent;
import com.deliveranything.domain.order.event.OrderPaymentFailedEvent;
import com.deliveranything.domain.order.event.OrderPaymentSucceededEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCancelFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCanceledForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCreateFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCreatedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPaidForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPaymentFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPreparingForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderStatusChangedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCancelFailedForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCanceledForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPaidForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPreparingForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderStatusChangedForSellerEvent;
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
public class OrderService {

  private final ApplicationEventPublisher eventPublisher;
  private final OrderRepository orderRepository;

  @Transactional
  public void processPaymentCompletion(String merchantUid) {
    Order order = getOrderWithStoreByMerchantId(merchantUid);
    eventPublisher.publishEvent(OrderPaymentSucceededEvent.fromOrder(order));
  }

  @Transactional
  public void processStockCommitted(Long orderId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.PENDING);

    eventPublisher.publishEvent(OrderPaidForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderPaidForSellerEvent.fromOrder(order));
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
  }

  @Transactional
  public void processStockReleased(Long orderId) {
    Order order = getOrderById(orderId);
    order.updateStatus(OrderStatus.PAYMENT_FAILED);

    eventPublisher.publishEvent(OrderPaymentFailedForCustomerEvent.fromOrder(order));
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
  }

  @Transactional
  public void processStockReplenished(Long orderId) {
    Order order = getOrderById(orderId);

    eventPublisher.publishEvent(OrderCanceledForCustomerEvent.fromOrder(order));
    eventPublisher.publishEvent(OrderCanceledForSellerEvent.fromOrder(order));
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

  @Transactional(readOnly = true)
  public void processStockReserved(Long orderId) {
    Order order = getOrderById(orderId);
    log.info("주문 재고 홀드 됨. 클라이언트에게 주문 생성 관련 정보 전달.");

    eventPublisher.publishEvent(OrderCreatedForCustomerEvent.fromOrder(order));
  }

  @Transactional
  public void processStockReserveFailed(Long orderId, String reason) {
    log.info("주문 [{}] 취소 처리 시작. 사유: 재고 예약 실패 ({})", orderId, reason);

    Order order = getOrderById(orderId);
    order.cancel(reason);
    eventPublisher.publishEvent(OrderCreateFailedForCustomerEvent.fromOrder(order));

    log.info("주문 [{}] 취소 처리 완료.", orderId);
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