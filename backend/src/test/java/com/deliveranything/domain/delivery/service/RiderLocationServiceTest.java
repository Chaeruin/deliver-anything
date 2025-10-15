package com.deliveranything.domain.delivery.service;

import static com.deliveranything.domain.delivery.service.RiderLocationService.RIDER_GEO_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.delivery.dto.RiderLocationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiderLocationService 단위 테스트")
class RiderLocationServiceTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @InjectMocks
  private RiderLocationService riderLocationService;

  @Test
  @DisplayName("라이더 위치 저장 - Redis GEOADD 호출 확인")
  void 라이더_위치_저장_테스트() {
    // Given
    Long riderProfileId = 123L;
    RiderLocationDto location = RiderLocationDto.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(System.currentTimeMillis())
        .build();

    GeoOperations<String, String> geoOps = mock(GeoOperations.class);
    when(redisTemplate.opsForGeo()).thenReturn(geoOps);

    // When
    riderLocationService.saveRiderLocation(riderProfileId, location);

    // Then: GEOADD 호출 검증 (longitude, latitude 순서)
    verify(geoOps).add(eq(RIDER_GEO_KEY), eq(new Point(126.9780, 37.5665)), eq(String.valueOf(riderProfileId)));
  }

  @Test
  @DisplayName("라이더 위치 저장 - 업데이트 시 마지막 좌표로 저장")
  void 라이더_위치_업데이트_테스트() {
    // Given
    Long riderProfileId = 456L;
    RiderLocationDto firstLocation = RiderLocationDto.builder()
        .latitude(37.5512)
        .longitude(126.9882)
        .timestamp(System.currentTimeMillis())
        .build();
    RiderLocationDto secondLocation = RiderLocationDto.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(System.currentTimeMillis())
        .build();

    GeoOperations<String, String> geoOps = mock(GeoOperations.class);
    when(redisTemplate.opsForGeo()).thenReturn(geoOps);

    // When
    riderLocationService.saveRiderLocation(riderProfileId, firstLocation);
    riderLocationService.saveRiderLocation(riderProfileId, secondLocation);

    // Then: 두 번 호출되고, 마지막 호출 값이 기대와 일치
    verify(geoOps, times(2)).add(eq(RIDER_GEO_KEY), any(Point.class), eq(String.valueOf(riderProfileId)));
    verify(geoOps).add(eq(RIDER_GEO_KEY), eq(new Point(126.9780, 37.5665)), eq(String.valueOf(riderProfileId)));
  }

  @Test
  @DisplayName("여러 라이더 위치 저장 - 각각 독립 저장")
  void 여러_라이더_위치_저장_테스트() {
    // Given
    RiderLocationDto location1 = RiderLocationDto.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(System.currentTimeMillis())
        .build();
    RiderLocationDto location2 = RiderLocationDto.builder()
        .latitude(37.5512)
        .longitude(126.9882)
        .timestamp(System.currentTimeMillis())
        .build();

    GeoOperations<String, String> geoOps = mock(GeoOperations.class);
    when(redisTemplate.opsForGeo()).thenReturn(geoOps);

    // When
    riderLocationService.saveRiderLocation(1L, location1);
    riderLocationService.saveRiderLocation(2L, location2);

    // Then: 각 라이더에 대해 GEOADD 호출 검증
    verify(geoOps).add(eq(RIDER_GEO_KEY), eq(new Point(126.9780, 37.5665)), eq("1"));
    verify(geoOps).add(eq(RIDER_GEO_KEY), eq(new Point(126.9882, 37.5512)), eq("2"));
  }
}
