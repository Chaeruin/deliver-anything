package com.deliveranything.domain.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.OrderPaymentSucceededEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.order.event.sse.customer.OrderPaidForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPaidForSellerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPreparingForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderPreparingForSellerEvent;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.order.event.OrderCancelSucceededEvent;
import com.deliveranything.domain.order.event.OrderCompletedEvent;
import com.deliveranything.domain.order.event.OrderPaymentFailedEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCancelFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCanceledForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCreateFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderCreatedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderPaymentFailedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.customer.OrderStatusChangedForCustomerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCancelFailedForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderCanceledForSellerEvent;
import com.deliveranything.domain.order.event.sse.seller.OrderStatusChangedForSellerEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

  @InjectMocks
  private OrderService orderService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("결제 완료 처리 성공")
  void processPaymentCompletion_success() {
    // given
    String merchantUid = "test-merchant-uid";
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order mockOrder = Order.builder().store(mockStore).build();

    given(orderRepository.findOrderWithStoreByMerchantId(merchantUid))
        .willReturn(Optional.of(mockOrder));

    // when
    orderService.processPaymentCompletion(merchantUid);

    // then
    then(eventPublisher).should(times(1)).publishEvent(any(OrderPaymentSucceededEvent.class));
  }

  @Test
  @DisplayName("재고 확정 처리 성공")
  void processStockCommitted_success() {
    // given
    Long orderId = 1L;
    Store mockStore = Store.builder().build();
    CustomerProfile mockCustomer = CustomerProfile.builder().build();
    Order realOrder = Order.builder().store(mockStore).customer(mockCustomer).build();
    Order spyOrder = Mockito.spy(realOrder);

    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processStockCommitted(orderId);

    // then
    then(spyOrder).should(times(1)).updateStatus(OrderStatus.PENDING);
    then(eventPublisher).should().publishEvent(any(OrderPaidForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderPaidForSellerEvent.class));
  }

  @Test
  @DisplayName("주문 전달 처리 성공")
  void processOrderTransmitted_success() {
    // given
    Long orderId = 1L;
    Store mockStore = Store.builder().build();
    CustomerProfile mockCustomer = CustomerProfile.builder().build();
    Order realOrder = Order.builder().store(mockStore).customer(mockCustomer).build();
    Order spyOrder = Mockito.spy(realOrder);

    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processOrderTransmitted(orderId);

    // then
    then(spyOrder).should(times(1)).updateStatus(OrderStatus.PREPARING);
    then(eventPublisher).should().publishEvent(any(OrderPreparingForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderPreparingForSellerEvent.class));
  }

  @Test
  @DisplayName("결제 실패 처리 성공")
  void processPaymentFailure_success() {
    // given
    String merchantUid = "test-merchant-uid";
    // FIX: Add nested mocks to prevent NPE in event creation
    Order mockOrder = Order.builder()
        .customer(CustomerProfile.builder().build())
        .store(Store.builder().build())
        .build();
    given(orderRepository.findByMerchantId(merchantUid)).willReturn(Optional.of(mockOrder));

    // when
    orderService.processPaymentFailure(merchantUid);

    // then
    then(eventPublisher).should().publishEvent(any(OrderPaymentFailedEvent.class));
  }

  @Test
  @DisplayName("재고 해제 처리 성공")
  void processStockReleased_success() {
    // given
    Long orderId = 1L;
    Order realOrder = Order.builder().customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processStockReleased(orderId);

    // then
    then(spyOrder).should().updateStatus(OrderStatus.PAYMENT_FAILED);
    then(eventPublisher).should().publishEvent(any(OrderPaymentFailedForCustomerEvent.class));
  }

  @ParameterizedTest
  @CsvSource({"CUSTOMER, CANCELED", "STORE, REJECTED"})
  @DisplayName("결제 취소 성공 처리")
  void processPaymentCancelSuccess_success(Publisher publisher, OrderStatus expectedStatus) {
    // given
    String merchantUid = "test-merchant-uid";
    // FIX: Add nested mocks to prevent NPE in event creation
    Order realOrder = Order.builder()
        .customer(CustomerProfile.builder().build())
        .store(Store.builder().build())
        .build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findByMerchantId(merchantUid)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processPaymentCancelSuccess(merchantUid, publisher);

    // then
    then(spyOrder).should().updateStatus(expectedStatus);
    then(eventPublisher).should().publishEvent(any(OrderCancelSucceededEvent.class));
  }

  @Test
  @DisplayName("재고 보충 처리 성공")
  void processStockReplenished_success() {
    // given
    Long orderId = 1L;
    Order mockOrder = Order.builder().store(Store.builder().build()).customer(CustomerProfile.builder().build()).build();
    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

    // when
    orderService.processStockReplenished(orderId);

    // then
    then(eventPublisher).should().publishEvent(any(OrderCanceledForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderCanceledForSellerEvent.class));
  }

  @Test
  @DisplayName("결제 취소 실패 처리 성공")
  void processPaymentCancelFailed_success() {
    // given
    String merchantUid = "test-merchant-uid";
    Order realOrder = Order.builder().store(Store.builder().build()).customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findByMerchantId(merchantUid)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processPaymentCancelFailed(merchantUid);

    // then
    then(spyOrder).should().updateStatus(OrderStatus.CANCEL_FAILED);
    then(eventPublisher).should().publishEvent(any(OrderCancelFailedForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderCancelFailedForSellerEvent.class));
  }

  @Test
  @DisplayName("라이더 배정 처리 성공")
  void processDeliveryRiderAssigned_success() {
    // given
    Long orderId = 1L;
    Order realOrder = Order.builder().store(Store.builder().build()).customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processDeliveryRiderAssigned(orderId);

    // then
    then(spyOrder).should().updateStatus(OrderStatus.RIDER_ASSIGNED);
    // FIX: Verify correct, distinct events
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForSellerEvent.class));
  }

  @Test
  @DisplayName("픽업 처리 성공")
  void processDeliveryPickedUp_success() {
    // given
    Long orderId = 1L;
    Order realOrder = Order.builder().store(Store.builder().build()).customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processDeliveryPickedUp(orderId);

    // then
    then(spyOrder).should().updateStatus(OrderStatus.DELIVERING);
    // FIX: Verify correct, distinct events
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForSellerEvent.class));
  }

  @Test
  @DisplayName("배달 완료 처리 성공")
  void processDeliveryCompleted_success() {
    // given
    Long orderId = 1L;
    Order realOrder = Order.builder().store(Store.builder().build()).customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processDeliveryCompleted(orderId, 1L, 2L);

    // then
    then(spyOrder).should().updateStatus(OrderStatus.COMPLETED);
    then(eventPublisher).should().publishEvent(any(OrderCompletedEvent.class));
    // FIX: Verify correct, distinct events
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForCustomerEvent.class));
    then(eventPublisher).should().publishEvent(any(OrderStatusChangedForSellerEvent.class));
  }

  @Test
  @DisplayName("재고 예약 처리 성공")
  void processStockReserved_success() {
    // given
    Long orderId = 1L;
    Order mockOrder = Order.builder().customer(CustomerProfile.builder().build()).build();
    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

    // when
    orderService.processStockReserved(orderId);

    // then
    then(eventPublisher).should().publishEvent(any(OrderCreatedForCustomerEvent.class));
  }

  @Test
  @DisplayName("재고 예약 실패 처리 성공")
  void processStockReserveFailed_success() {
    // given
    Long orderId = 1L;
    String reason = "재고 부족";
    Order realOrder = Order.builder().customer(CustomerProfile.builder().build()).build();
    Order spyOrder = Mockito.spy(realOrder);
    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    orderService.processStockReserveFailed(orderId, reason);

    // then
    then(spyOrder).should().cancel(reason);
    then(eventPublisher).should().publishEvent(any(OrderCreateFailedForCustomerEvent.class));
  }
}
