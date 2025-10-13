package com.deliveranything.domain.delivery.service;

import com.deliveranything.domain.delivery.dto.OrderDetailsDto;
import com.deliveranything.domain.delivery.event.dto.RiderNotificationDto;
import com.deliveranything.domain.order.enums.OrderStatus;
import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Virtual Thread 기반 주문 알림 처리 서비스
 * - 주문 접수 시 반경 내 라이더에게 알림 전송
 * - 블로킹 방식이지만 Virtual Thread에서 효율적으로 동작
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

  private final ReactiveRiderEtaService reactiveRiderEtaService;
  private final EtaService etaService;

  /**
   * 주문 이벤트 처리 (동기식, Virtual Thread에서 실행)
   * - 상점-고객 거리 계산
   * - 반경 내 라이더 조회 및 ETA 계산
   * - 라이더별 알림 DTO 생성
   */
  public List<RiderNotificationDto> processOrderEvent(OrderAcceptedEvent orderEvent) {
    double storeLat = orderEvent.storeLat();
    double storeLon = orderEvent.storeLon();
    double customerLat = orderEvent.customerLat();
    double customerLon = orderEvent.customerLon();
    String orderId = orderEvent.orderId();

    // 1. 상점 → 고객 거리 계산 (블로킹, but Virtual Thread)
    Map<String, Double> distanceMap = etaService.getDistance(
        storeLat, storeLon, customerLat, customerLon
    );
    
    double distanceKm = distanceMap.getOrDefault("distance", 0.0);
    
    // 2. 예상 배송비 계산
    int expectedCharge = 3000;
    if (distanceKm > 3.0) {
      expectedCharge += (int) Math.ceil((distanceKm - 3.0) * 1000); // 3km 초과 시 추가 요금
    }
    
    // 3. 반경 내 라이더 ETA 조회 (블로킹, but Virtual Thread)
    Map<String, Double> etaMap = reactiveRiderEtaService.findNearbyRidersEta(
        customerLat, customerLon, 3.0
    );

    if (etaMap.isEmpty()) {
      return new ArrayList<>();
    }

    // 4. 라이더별 알림 DTO 생성
    List<RiderNotificationDto> dtoList = new ArrayList<>();
    int finalExpectedCharge = expectedCharge;

    etaMap.forEach((riderId, etaMinutes) -> {
      RiderNotificationDto dto = RiderNotificationDto.builder()
          .orderDetailsDto(
              OrderDetailsDto.builder()
                  .orderId(orderId)
                  .storeName(orderEvent.storeName())
                  .distance(distanceKm)
                  .expectedCharge(finalExpectedCharge)
                  .build()
          )
          .riderId(riderId)
          .etaMinutes(etaMinutes)
          .orderDeliveryStatus(OrderStatus.RIDER_ASSIGNED)
          .build();
      dtoList.add(dto);
    });
    
    return dtoList;
  }
}
