package com.deliveranything.domain.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.event.OrderPaymentRequestedEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.OrderCancelEvent;
import com.deliveranything.domain.order.enums.Publisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrderService 테스트")
class PaymentOrderServiceTest {

  @InjectMocks
  private PaymentOrderService paymentOrderService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("주문 결제 요청 성공")
  void payOrder_success() {
    // given
    String merchantUid = "test-merchant-uid";
    String paymentKey = "test-payment-key";
    Order realOrder = Order.builder().build();
    Order spyOrder = Mockito.spy(realOrder);

    given(orderRepository.findByMerchantId(merchantUid)).willReturn(Optional.of(spyOrder));

    // when
    paymentOrderService.payOrder(merchantUid, paymentKey);

    // then
    then(spyOrder).should(times(1)).isPayable();
    then(eventPublisher).should(times(1)).publishEvent(any(OrderPaymentRequestedEvent.class));
  }

  @Test
  @DisplayName("주문 취소 요청 성공")
  void cancelOrder_success() {
    // given
    Long orderId = 1L;
    String cancelReason = "고객 변심";
    Order realOrder = Order.builder().build();
    // Set initial status to PENDING for cancellation to be possible
    realOrder.updateStatus(OrderStatus.PENDING);
    Order spyOrder = Mockito.spy(realOrder);

    given(orderRepository.findById(orderId)).willReturn(Optional.of(spyOrder));

    // when
    paymentOrderService.cancelOrder(orderId, cancelReason);

    // then
    then(spyOrder).should(times(1)).cancellationRequest(cancelReason);
    then(eventPublisher).should(times(1)).publishEvent(any(OrderCancelEvent.class));
  }
}
