package com.deliveranything.domain.delivery.service;

import static com.deliveranything.domain.delivery.service.RiderLocationService.RIDER_GEO_KEY;

import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import com.deliveranything.domain.user.profile.entity.RiderProfile;
import com.deliveranything.domain.user.profile.enums.RiderToggleStatus;
import com.deliveranything.domain.user.profile.repository.RiderProfileRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

/**
 * Virtual Thread 기반 라이더 ETA 서비스 - 반경 내 라이더 조회 (Redis GEOSEARCH) - ETA 계산 (Kakao API, 병렬 처리) - 블로킹
 * 방식이지만 Virtual Thread에서 효율적으로 동작
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveRiderEtaService {

  private final StringRedisTemplate redisTemplate;
  private final EtaService etaService;
  private final RiderProfileRepository riderProfileRepository;

  /**
   * 반경 내 라이더 검색 후 ETA 계산 (동기식) - Redis GEOSEARCH로 반경 내 라이더 조회
   *
   * @return Map<riderId, etaMinutes>
   */
  public Map<String, Double> findNearbyRidersEta(
      OrderAcceptedEvent order, double radiusKm
  ) {
    double customerLat = order.customerLat();
    double customerLon = order.customerLon();
    // 1. Redis GEOSEARCH로 반경 내 라이더 조회
    GeoResults<RedisGeoCommands.GeoLocation<String>> nearbyRiders =
        redisTemplate.opsForGeo().search(
            RIDER_GEO_KEY,
            GeoReference.fromCoordinate(customerLat, customerLon),
            new Distance(radiusKm, Metrics.KILOMETERS),
            RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()
        );

    if (nearbyRiders == null || nearbyRiders.getContent().isEmpty()) {
      return new HashMap<>();
    }

    // 2. ON 상태 라이더만 필터링
    List<String> riderIds = new ArrayList<>();
    List<Point> riderPoints = new ArrayList<>();

    for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : nearbyRiders) {
      RedisGeoCommands.GeoLocation<String> loc = result.getContent();

      try {
        RiderProfile riderProfile = riderProfileRepository.findById(Long.parseLong(loc.getName()))
            .orElseThrow(() -> new CustomException(ErrorCode.RIDER_NOT_FOUND));

        if (riderProfile.getToggleStatus() == RiderToggleStatus.OFF) {
          continue; // OFF 상태 라이더는 제외
        }

        riderIds.add(loc.getName());
        riderPoints.add(loc.getPoint());
      } catch (CustomException e) {
        continue;
      }
    }

    if (riderIds.isEmpty()) {
      return new HashMap<>();
    }

    // 3. Kakao Map API로 ETA 계산 (병렬 처리, Virtual Thread)
    Map<String, Double> etaMap = etaService.getEtaForMultiple(
        order, riderPoints, riderIds
    );

    return etaMap;
  }
}