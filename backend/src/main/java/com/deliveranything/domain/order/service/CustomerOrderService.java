package com.deliveranything.domain.order.service;

import com.deliveranything.domain.order.dto.OrderCreateRequest;
import com.deliveranything.domain.order.dto.OrderItemRequest;
import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.entity.Order;
import com.deliveranything.domain.order.entity.OrderItem;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.OrderCreatedEvent;
import com.deliveranything.domain.order.repository.OrderRepository;
import com.deliveranything.domain.order.repository.OrderRepositoryCustom;
import com.deliveranything.domain.product.product.service.ProductService;
import com.deliveranything.domain.store.store.service.StoreService;
import com.deliveranything.domain.user.profile.service.CustomerProfileService;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import com.deliveranything.global.util.CursorUtil;
import com.deliveranything.global.util.PointUtil;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CustomerOrderService {

  private final CustomerProfileService customerProfileService;
  private final ProductService productService;
  private final StoreService storeService;

  private final OrderRepository orderRepository;
  private final OrderRepositoryCustom orderRepositoryCustom;

  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public OrderResponse createOrder(Long customerId, OrderCreateRequest orderCreateRequest) {
    Order order = Order.builder()
        .customer(customerProfileService.getProfile(customerId))
        .store(storeService.getStoreById(orderCreateRequest.storeId()))
        .address(orderCreateRequest.address())
        .destination(PointUtil.createPoint(orderCreateRequest.lat(), orderCreateRequest.lng()))
        .riderNote(orderCreateRequest.riderNote())
        .storeNote(orderCreateRequest.storeNote())
        .totalPrice(orderCreateRequest.totalPrice())
        .storePrice(orderCreateRequest.storePrice())
        .deliveryPrice(orderCreateRequest.deliveryPrice())
        .build();

    for (OrderItemRequest orderItemRequest : orderCreateRequest.orderItemRequests()) {
      OrderItem orderItem = OrderItem.builder()
          .product(productService.getProductById(orderItemRequest.productId()))
          .price(orderItemRequest.price())
          .quantity(orderItemRequest.quantity())
          .build();

      order.addOrderItem(orderItem);
    }

    Order savedOrder = orderRepository.save(order);
    // TODO: 재고가 없다면 주문 CANCELED나 재고 없음 상태로 이벤트 들어서 바꿔야함.
    // TODO: 재고가 확인됐다는 이벤트 받으면 재고Sub이 알림 호출해서 SSE로 클랄 전달
    eventPublisher.publishEvent(OrderCreatedEvent.from(savedOrder));

    return OrderResponse.from(savedOrder);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<OrderResponse> getCustomerOrdersByCursor(
      Long customerId,
      Long cursor,
      int size
  ) {
    List<Order> orders = orderRepositoryCustom.findOrdersWithStoreByCustomerId(customerId, cursor,
        size + 1);

    List<OrderResponse> orderResponses = orders.stream()
        .limit(size)
        .map(OrderResponse::from)
        .toList();

    boolean hasNext = orders.size() > size;

    return new CursorPageResponse<>(
        orderResponses,
        hasNext ? orderResponses.getLast().id().toString() : null,
        hasNext
    );
  }

  @Transactional(readOnly = true)
  public OrderResponse getCustomerOrder(Long orderId, Long customerId) {
    return OrderResponse.from(
        orderRepository.findOrderWithStoreByIdAndCustomerId(orderId, customerId)
            .orElseThrow(() -> new CustomException(ErrorCode.CUSTOMER_ORDER_NOT_FOUND)));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> getProgressingOrders(Long customerId) {
    return orderRepository.findOrdersWithStoreByCustomerIdAndStatuses(customerId, List.of(
            OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.RIDER_ASSIGNED,
            OrderStatus.DELIVERING)).stream()
        .map(OrderResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<OrderResponse> getCompletedOrdersByCursor(
      Long customerId,
      String nextPageToken,
      int size
  ) {
    LocalDateTime lastCreatedAt = null;
    Long lastOrderId = null;
    String[] decodedParts = (String[]) CursorUtil.decode(nextPageToken);

    if (decodedParts != null && decodedParts.length == 2) {
      try {
        lastCreatedAt = LocalDateTime.parse(decodedParts[0]);
        lastOrderId = Long.parseLong(decodedParts[1]);
      } catch (NumberFormatException e) {
        lastCreatedAt = null;
        lastOrderId = null;
      }
    }

    List<Order> cursorOrders = orderRepositoryCustom.findOrdersWithStoreByCustomerId(customerId,
        List.of(OrderStatus.COMPLETED), lastCreatedAt, lastOrderId, size + 1);

    List<OrderResponse> cursorResponses = cursorOrders.stream()
        .limit(size)
        .map(OrderResponse::from)
        .toList();

    boolean hasNext = cursorOrders.size() > size;
    OrderResponse lastResponse = cursorResponses.getLast();

    return new CursorPageResponse<>(
        cursorResponses,
        hasNext ? CursorUtil.encode(lastResponse.createdAt(), lastResponse.id()) : null,
        hasNext
    );
  }
}
