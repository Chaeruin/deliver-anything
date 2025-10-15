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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderOrderService 테스트")
class RiderOrderServiceTest {

  @InjectMocks
  private RiderOrderService riderOrderService;

  @Mock
  private OrderRepository orderRepository;

  @Test
  @DisplayName("주문 조회 성공")
  void getOrder_success() {
    // given
    Long orderId = 1L;
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order mockOrder = Order.builder()
        .store(mockStore)
        .customer(CustomerProfile.builder().build())
        .address("테스트 주소")
        .build();

    given(orderRepository.findById(orderId))
        .willReturn(Optional.of(mockOrder));

    // when
    OrderResponse response = riderOrderService.getOrder(orderId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.storeName()).isEqualTo("테스트 가게");
    assertThat(response.address()).isEqualTo("테스트 주소");
  }

  @Test
  @DisplayName("주문 조회 실패 - 주문 없음")
  void getOrder_notFound() {
    // given
    Long orderId = 1L;
    given(orderRepository.findById(orderId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> riderOrderService.getOrder(orderId))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
  }
}
