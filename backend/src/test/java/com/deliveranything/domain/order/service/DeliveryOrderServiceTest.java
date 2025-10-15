package com.deliveranything.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryOrderService 테스트")
class DeliveryOrderServiceTest {

  @InjectMocks
  private DeliveryOrderService deliveryOrderService;

  @Mock
  private OrderRepository orderRepository;

  @Test
  @DisplayName("배달 ID로 주문 조회 성공")
  void getOrderByDeliveryId_success() {
    // given
    Long deliveryId = 1L;
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order mockOrder = Order.builder()
        .store(mockStore)
        .customer(CustomerProfile.builder().build())
        .address("테스트 주소")
        .build();

    given(orderRepository.findOrderWithStoreByDeliveryId(deliveryId))
        .willReturn(Optional.of(mockOrder));

    // when
    OrderResponse response = deliveryOrderService.getOrderByDeliveryId(deliveryId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.storeName()).isEqualTo("테스트 가게");
  }

  @Test
  @DisplayName("배달 ID로 주문 조회 실패 - 주문 없음")
  void getOrderByDeliveryId_notFound() {
    // given
    Long deliveryId = 1L;
    given(orderRepository.findOrderWithStoreByDeliveryId(deliveryId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> deliveryOrderService.getOrderByDeliveryId(deliveryId))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("라이더의 배달 목록 조회 성공")
  void getRiderDeliveryOrders_success() {
    // given
    Long riderProfileId = 1L;
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order order1 = Order.builder().store(mockStore).address("주소 1").build();
    Order order2 = Order.builder().store(mockStore).address("주소 2").build();
    List<Order> mockOrders = List.of(order1, order2);

    given(orderRepository.findOrdersWithStoreByRiderProfile(riderProfileId))
        .willReturn(mockOrders);

    // when
    List<OrderResponse> responses = deliveryOrderService.getRiderDeliveryOrders(riderProfileId);

    // then
    assertThat(responses).isNotNull();
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).address()).isEqualTo("주소 1");
    assertThat(responses.get(1).address()).isEqualTo("주소 2");
  }

  @Test
  @DisplayName("주문 ID로 Order 엔티티 조회 성공")
  void getOrderById_success() {
    // given
    Long orderId = 1L;
    Order mockOrder = Order.builder().address("some address").build();
    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

    // when
    Order result = deliveryOrderService.getOrderById(orderId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getAddress()).isEqualTo("some address");
  }

  @Test
  @DisplayName("주문 ID로 Order 엔티티 조회 실패 - 주문 없음")
  void getOrderById_notFound() {
    // given
    Long orderId = 1L;
    given(orderRepository.findById(orderId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> deliveryOrderService.getOrderById(orderId))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("주문 ID로 고객 ID 조회 성공")
  void getCustomerIdByOrderId_success() {
    // given
    Long orderId = 1L;
    Long expectedCustomerId = 100L;

    CustomerProfile mockCustomerWithId = org.mockito.Mockito.mock(CustomerProfile.class);
    given(mockCustomerWithId.getId()).willReturn(expectedCustomerId);
    Order mockOrderWithCustomer = Order.builder().customer(mockCustomerWithId).build();
    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrderWithCustomer));

    // when
    Long customerId = deliveryOrderService.getCustomerIdByOrderId(orderId);

    // then
    assertThat(customerId).isEqualTo(expectedCustomerId);
  }

  @Test
  @DisplayName("주문 ID로 판매자 ID 조회 성공")
  void getSellerIdByOrderId_success() {
    // given
    Long orderId = 1L;
    Long expectedSellerId = 200L;

    Store mockStoreWithSeller = Store.builder().sellerProfileId(expectedSellerId).build();
    Order mockOrderWithStore = Order.builder().store(mockStoreWithSeller).build();
    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrderWithStore));

    // when
    Long sellerId = deliveryOrderService.getSellerIdByOrderId(orderId);

    // then
    assertThat(sellerId).isEqualTo(expectedSellerId);
  }
}
