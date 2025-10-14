package com.deliveranything.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.delivery.event.dto.RiderNotificationDto;
import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderNotificationService 단위 테스트")
class OrderNotificationServiceTest {

  @InjectMocks
  private OrderNotificationService orderNotificationService;

  @Mock
  private ReactiveRiderEtaService reactiveRiderEtaService;

  @Mock
  private EtaService etaService;

  @Test
  @DisplayName("배송비 계산 - 기본 거리 (3km 이하)")
  void 배송비_계산_기본거리_테스트() {
    // Given: 거리 2km
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("distance", 2.0));
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("rider1", 15.0));

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 3000원 (기본 요금)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).orderDetailsDto().expectedCharge()).isEqualTo(3000);
    assertThat(result.get(0).orderDetailsDto().distance()).isEqualTo(2.0);
    assertThat(result.get(0).riderId()).isEqualTo("rider1");
    assertThat(result.get(0).etaMinutes()).isEqualTo(15.0);
  }

  @Test
  @DisplayName("배송비 계산 - 초과 거리 (3km 초과)")
  void 배송비_계산_초과거리_테스트() {
    // Given: 거리 5km
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("distance", 5.0));
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("rider1", 20.0));

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 5000원 (3000 + 2000)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).orderDetailsDto().expectedCharge()).isEqualTo(5000);
    assertThat(result.get(0).orderDetailsDto().distance()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("배송비 계산 - 소수점 거리")
  void 배송비_계산_소수점거리_테스트() {
    // Given: 거리 4.3km
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("distance", 4.3));
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("rider1", 18.0));

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 4300원 (3000 + 1300, Math.ceil(1.3) = 2)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).orderDetailsDto().expectedCharge()).isEqualTo(4300);
  }

  @Test
  @DisplayName("라이더 없음 - 빈 리스트 반환")
  void 라이더_없음_테스트() {
    // Given: 라이더 없음
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("distance", 2.0));
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of());

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 빈 리스트
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("여러 라이더 - 각각 다른 ETA")
  void 여러_라이더_테스트() {
    // Given: 2명의 라이더
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("distance", 2.0));
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of(
            "rider1", 15.0,
            "rider2", 25.0
        ));

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 2명의 라이더
    assertThat(result).hasSize(2);

    // 라이더별 검증
    boolean hasRider1 = result.stream()
        .anyMatch(r -> r.riderId().equals("rider1") && r.etaMinutes().equals(15.0));
    boolean hasRider2 = result.stream()
        .anyMatch(r -> r.riderId().equals("rider2") && r.etaMinutes().equals(25.0));

    assertThat(hasRider1).isTrue();
    assertThat(hasRider2).isTrue();
  }

  @Test
  @DisplayName("거리 정보가 없을 때 기본값 처리")
  void 거리정보_없음_테스트() {
    // Given: 거리 정보 없음
    OrderAcceptedEvent event = createOrderEvent();

    // Mock 설정
    when(etaService.getDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of()); // 빈 맵
    when(reactiveRiderEtaService.findNearbyRidersEta(anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(Map.of("rider1", 15.0));

    // When
    List<RiderNotificationDto> result = orderNotificationService.processOrderEvent(event);

    // Then: 기본 배송비 3000원
    assertThat(result).hasSize(1);
    assertThat(result.get(0).orderDetailsDto().expectedCharge()).isEqualTo(3000);
    assertThat(result.get(0).orderDetailsDto().distance()).isEqualTo(0.0);
  }

  private OrderAcceptedEvent createOrderEvent() {
    List<OrderItemInfo> orderItems = new ArrayList<>();
    orderItems.add(new OrderItemInfo(1L, 2));
    return new OrderAcceptedEvent(
        "order123",
        orderItems,
        "맛있는 치킨집",
        37.5, 127.0, // store
        37.6, 127.1  // customer
    );
  }
}
