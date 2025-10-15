package com.deliveranything.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import com.deliveranything.domain.order.event.OrderRejectedEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.domain.order.repository.OrderRepositoryCustom;
import com.deliveranything.domain.store.store.entity.Store;
import com.deliveranything.domain.store.category.entity.StoreCategory;
import com.deliveranything.domain.user.profile.entity.CustomerProfile;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.util.PointUtil;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreOrderService 테스트")
class StoreOrderServiceTest {

  @InjectMocks
  private StoreOrderService storeOrderService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderRepositoryCustom orderRepositoryCustom;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Test
  @DisplayName("상점 주문 목록 커서 기반 조회 성공")
  void getStoreOrdersByCursor_success() {
    // given
    Long storeId = 1L;
    int size = 5;

    List<Order> mockOrders = IntStream.range(0, size + 1)
        .mapToObj(i -> {
          StoreCategory mockStoreCategory = new StoreCategory("카테고리");
          Store mockStore = Store.builder()
              .sellerProfileId(2L)
              .storeCategory(mockStoreCategory)
              .imageUrl("http://store.image.com")
              .name("테스트 가게 " + i)
              .roadAddr("테스트 도로명 주소")
              .location(PointUtil.createPoint(127.0, 37.0))
              .build();
          CustomerProfile mockCustomer = CustomerProfile.builder().build();
          Point mockPoint = PointUtil.createPoint(127.0 + i, 37.0 + i);

          Order order = Order.builder()
              .customer(mockCustomer)
              .store(mockStore)
              .address("주소 " + i)
              .destination(mockPoint)
              .riderNote("라이더 메모 " + i)
              .storeNote("상점 메모 " + i)
              .totalPrice(1000L + i)
              .storePrice(900L + i)
              .deliveryPrice(100L + i)
              .build();
          order.updateStatus(OrderStatus.COMPLETED);
          return order;
        })
        .collect(Collectors.toList());

    given(orderRepositoryCustom.findOrdersWithStoreByStoreId(anyLong(), any(List.class), nullable(LocalDateTime.class), nullable(Long.class), anyInt()))
        .willReturn(mockOrders);

    // when
    CursorPageResponse<OrderResponse> response = storeOrderService.getStoreOrdersByCursor(storeId, null, size);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.content()).hasSize(size);
    assertThat(response.nextPageToken()).isNotNull();
    assertThat(response.content().get(0).storeName()).isEqualTo("테스트 가게 0");
  }

  @Test
  @DisplayName("대기중인 주문 목록 조회 성공")
  void getPendingOrders_success() {
    // given
    Long storeId = 1L;
    StoreCategory mockStoreCategory = new StoreCategory("카테고리");
    Store mockStore = Store.builder()
        .sellerProfileId(2L)
        .storeCategory(mockStoreCategory)
        .imageUrl("http://store.image.com")
        .name("테스트 가게")
        .roadAddr("테스트 도로명 주소")
        .location(PointUtil.createPoint(127.0, 37.0))
        .build();
    CustomerProfile mockCustomer = CustomerProfile.builder().build();
    Point mockPoint = PointUtil.createPoint(127.0, 37.0);

    Order order1 = Order.builder()
        .customer(mockCustomer)
        .store(mockStore)
        .address("주소 1")
        .destination(mockPoint)
        .riderNote("라이더 메모")
        .storeNote("상점 메모")
        .totalPrice(1000L)
        .storePrice(900L)
        .deliveryPrice(100L)
        .build();
    order1.updateStatus(OrderStatus.PENDING);

    Order order2 = Order.builder()
        .customer(mockCustomer)
        .store(mockStore)
        .address("주소 2")
        .destination(mockPoint)
        .riderNote("라이더 메모")
        .storeNote("상점 메모")
        .totalPrice(2000L)
        .storePrice(1800L)
        .deliveryPrice(200L)
        .build();
    order2.updateStatus(OrderStatus.PENDING);

    List<Order> mockOrders = List.of(order1, order2);

    given(orderRepository.findOrdersWithStoreByStoreIdAndStatus(storeId, OrderStatus.PENDING))
        .willReturn(mockOrders);

    // when
    List<OrderResponse> responses = storeOrderService.getPendingOrders(storeId);

    // then
    assertThat(responses).isNotNull();
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).address()).isEqualTo("주소 1");
    assertThat(responses.get(1).address()).isEqualTo("주소 2");
  }

  @Test
  @DisplayName("수락된 주문 목록 조회 성공")
  void getAcceptedOrders_success() {
    // given
    Long storeId = 1L;
    StoreCategory mockStoreCategory = new StoreCategory("카테고리");
    Store mockStore = Store.builder()
        .sellerProfileId(2L)
        .storeCategory(mockStoreCategory)
        .imageUrl("http://store.image.com")
        .name("테스트 가게")
        .roadAddr("테스트 도로명 주소")
        .location(PointUtil.createPoint(127.0, 37.0))
        .build();
    CustomerProfile mockCustomer = CustomerProfile.builder().build();
    Point mockPoint = PointUtil.createPoint(127.0, 37.0);

    Order order1 = Order.builder()
        .customer(mockCustomer)
        .store(mockStore)
        .address("주소 1")
        .destination(mockPoint)
        .riderNote("라이더 메모")
        .storeNote("상점 메모")
        .totalPrice(1000L)
        .storePrice(900L)
        .deliveryPrice(100L)
        .build();
    order1.updateStatus(OrderStatus.PREPARING);

    Order order2 = Order.builder()
        .customer(mockCustomer)
        .store(mockStore)
        .address("주소 2")
        .destination(mockPoint)
        .riderNote("라이더 메모")
        .storeNote("상점 메모")
        .totalPrice(2000L)
        .storePrice(1800L)
        .deliveryPrice(200L)
        .build();
    order2.updateStatus(OrderStatus.RIDER_ASSIGNED);

    List<Order> mockOrders = List.of(order1, order2);

    List<OrderStatus> acceptedStatuses = List.of(
        OrderStatus.PREPARING, OrderStatus.RIDER_ASSIGNED, OrderStatus.DELIVERING
    );

    given(orderRepository.findOrdersWithStoreByStoreIdAndStatuses(storeId, acceptedStatuses))
        .willReturn(mockOrders);

    // when
    List<OrderResponse> responses = storeOrderService.getAcceptedOrders(storeId);

    // then
    assertThat(responses).isNotNull();
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).address()).isEqualTo("주소 1");
    assertThat(responses.get(1).address()).isEqualTo("주소 2");
  }

  @Test
  @DisplayName("주문 수락 성공")
  void acceptOrder_success() {
    // given
    Long orderId = 1L;

    // Mock nested objects
    Point mockStoreLocation = Mockito.mock(Point.class);
    given(mockStoreLocation.getX()).willReturn(127.0);
    given(mockStoreLocation.getY()).willReturn(37.0);

    Store mockStore = Mockito.mock(Store.class);
    given(mockStore.getLocation()).willReturn(mockStoreLocation);

    Point mockOrderDestination = Mockito.mock(Point.class);
    given(mockOrderDestination.getX()).willReturn(127.0);
    given(mockOrderDestination.getY()).willReturn(37.0);

    // Mock the Order object directly
    Order mockOrder = Mockito.mock(Order.class);
    given(mockOrder.getDestination()).willReturn(mockOrderDestination);
    given(mockOrder.getStore()).willReturn(mockStore);
    // given(mockOrder.getId()).willReturn(orderId); // Removed as it might be unnecessary
    // given(mockOrder.getStatus()).willReturn(OrderStatus.PENDING); // Removed as it might be unnecessary

    given(orderRepository.findOrderWithStoreById(orderId)).willReturn(Optional.of(mockOrder));

    // when
    storeOrderService.acceptOrder(orderId);

    // then
    then(eventPublisher).should(times(1)).publishEvent(any(OrderAcceptedEvent.class));
  }

  @Test
  @DisplayName("주문 거절 성공")
  void rejectOrder_success() {
    // given
    Long orderId = 1L;
    String STORE_CANCEL_REASON = "상점이 주문을 거절했습니다.";

    // Mock the Order object directly
    Order mockOrder = Mockito.mock(Order.class);
    // given(mockOrder.getId()).willReturn(orderId); // Removed as it might be unnecessary
    // given(mockOrder.getStatus()).willReturn(OrderStatus.PENDING); // Removed as it might be unnecessary

    given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));

    // when
    storeOrderService.rejectOrder(orderId);

    // then
    then(mockOrder).should(times(1)).cancellationRequest(STORE_CANCEL_REASON);
    then(eventPublisher).should(times(1)).publishEvent(any(OrderRejectedEvent.class));
  }
}
