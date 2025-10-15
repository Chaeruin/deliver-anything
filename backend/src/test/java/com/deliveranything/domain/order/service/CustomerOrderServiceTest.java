package com.deliveranything.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.deliveranything.domain.order.dto.OrderCreateRequest;
import com.deliveranything.domain.order.dto.OrderItemRequest;
import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.domain.order.repository.OrderRepositoryCustom;
import com.deliveranything.domain.product.product.entity.Product;
import com.deliveranything.domain.product.product.service.ProductService;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerOrderService 테스트")
class CustomerOrderServiceTest {

  @InjectMocks
  private CustomerOrderService customerOrderService;

  @Mock
  private CustomerProfileService customerProfileService;
  @Mock
  private ProductService productService;
  @Mock
  private StoreService storeService;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private OrderRepositoryCustom orderRepositoryCustom;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("주문 생성 성공")
  void createOrder_success() {
    // given
    Long customerId = 1L;
    OrderItemRequest orderItemRequest = new OrderItemRequest(1L, 10000, 2);
    OrderCreateRequest createRequest = new OrderCreateRequest(
        1L, List.of(orderItemRequest), "서울시 강남구", 37.123, 127.123,
        "조심히 와주세요", "리뷰이벤트 참여", 23000L, 20000L, 3000L
    );

    CustomerProfile customerProfile = CustomerProfile.builder().build();
    Store store = Store.builder().build();
    Product product = Product.builder()
        .name("테스트 상품").price(10000).store(store).imageUrl("http://example.com/image.jpg")
        .initialStock(10)
        .build();

    // Mocking
    given(customerProfileService.getProfileByProfileId(any(Long.class))).willReturn(
        customerProfile);
    given(storeService.getStoreById(any(Long.class))).willReturn(store);
    given(productService.getProductById(any(Long.class))).willReturn(product);
    given(orderRepository.save(any(Order.class))).willAnswer(
        invocation -> invocation.getArgument(0));

    // when
    customerOrderService.createOrder(customerId, createRequest);

    // then
    then(orderRepository).should(times(1)).save(any(Order.class));
  }

  @Test
  @DisplayName("특정 주문 조회 성공")
  void getCustomerOrder_success() {
    // given
    Long orderId = 1L;
    Long customerId = 1L;
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order mockOrder = Order.builder()
        .store(mockStore)
        .customer(CustomerProfile.builder().build())
        .address("테스트 주소")
        .totalPrice(10000L)
        .storePrice(8000L)
        .deliveryPrice(2000L)
        .build();

    given(orderRepository.findOrderWithStoreByIdAndCustomerId(orderId, customerId))
        .willReturn(Optional.of(mockOrder));

    // when
    OrderResponse response = customerOrderService.getCustomerOrder(orderId, customerId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.storeName()).isEqualTo("테스트 가게");
    assertThat(response.address()).isEqualTo("테스트 주소");
  }

  @Test
  @DisplayName("특정 주문 조회 실패 - 주문을 찾을 수 없음")
  void getCustomerOrder_notFound() {
    // given
    Long orderId = 1L;
    Long customerId = 1L;

    given(orderRepository.findOrderWithStoreByIdAndCustomerId(orderId, customerId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> customerOrderService.getCustomerOrder(orderId, customerId))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.CUSTOMER_ORDER_NOT_FOUND.getMessage());
  }

  @Test
  @DisplayName("주문 목록 커서 기반 조회 성공")
  void getCustomerOrdersByCursor_success() {
    // given
    Long customerId = 1L;
    int size = 5;
    Store mockStore = Store.builder().name("테스트 가게").build();
    List<Order> mockOrders = IntStream.range(0, size + 1)
        .mapToObj(i -> Order.builder().store(mockStore).customer(CustomerProfile.builder().build())
            .address("주소 " + i).totalPrice(1000L).build())
        .toList();

    given(orderRepositoryCustom.findOrdersWithStoreByCustomerId(anyLong(), any(), anyInt()))
        .willReturn(mockOrders);

    // when
    CursorPageResponse<OrderResponse> response = customerOrderService.getCustomerOrdersByCursor(
        customerId, null, size);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.content()).hasSize(size);
    assertThat(response.nextPageToken()).isNotNull();
  }

  @Test
  @DisplayName("진행중인 주문 목록 조회 성공")
  void getProgressingOrders_success() {
    // given
    Long customerId = 1L;
    Store mockStore = Store.builder().name("테스트 가게").build();
    Order order1 = Order.builder().store(mockStore).customer(CustomerProfile.builder().build())
        .address("주소 1").totalPrice(1000L).build();
    Order order2 = Order.builder().store(mockStore).customer(CustomerProfile.builder().build())
        .address("주소 2").totalPrice(2000L).build();
    List<Order> mockOrders = List.of(order1, order2);

    List<OrderStatus> progressingStatuses = List.of(
        OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.RIDER_ASSIGNED,
        OrderStatus.DELIVERING
    );

    given(
        orderRepository.findOrdersWithStoreByCustomerIdAndStatuses(customerId, progressingStatuses))
        .willReturn(mockOrders);

    // when
    List<OrderResponse> responses = customerOrderService.getProgressingOrders(customerId);

    // then
    assertThat(responses).isNotNull();
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).address()).isEqualTo("주소 1");
  }

  @Test
  @DisplayName("완료된 주문 목록 커서 기반 조회 성공")
  void getCompletedOrdersByCursor_success() {
    // given
    Long customerId = 1L;
    int size = 5;
    Store mockStore = Store.builder().name("테스트 가게").build();
    List<Order> mockOrders = IntStream.range(0, size + 1)
        .mapToObj(i -> Order.builder().store(mockStore).customer(CustomerProfile.builder().build())
            .address("주소 " + i).totalPrice(1000L).build())
        .toList();

    given(orderRepositoryCustom.findOrdersWithStoreByCustomerId(anyLong(), any(List.class), any(),
        any(), anyInt()))
        .willReturn(mockOrders);

    // when
    CursorPageResponse<OrderResponse> response = customerOrderService.getCompletedOrdersByCursor(
        customerId, null, size);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.content()).hasSize(size);
    assertThat(response.nextPageToken()).isNotNull();
  }
}
