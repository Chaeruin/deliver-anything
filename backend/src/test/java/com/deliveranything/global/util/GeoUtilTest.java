package com.deliveranything.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoUtilTest {

  private static final double DELTA = 0.001; // 허용 오차

  @Test
  @DisplayName("두 지점 간의 거리 계산 테스트")
  void distanceKmTest() {
    // 서울 (37.5665, 126.9780) 와 부산 (35.1796, 129.0756) 사이의 대략적인 거리 (km)
    double seoulLat = 37.5665;
    double seoulLng = 126.9780;
    double busanLat = 35.1796;
    double busanLng = 129.0756;
    double expectedDistance = 325.0; // 실제 값과 다를 수 있으나, 대략적인 값으로 테스트

    double distance = GeoUtil.distanceKm(seoulLat, seoulLng, busanLat, busanLng);
    assertThat(distance).isCloseTo(expectedDistance, org.assertj.core.api.Assertions.within(50.0)); // 50km 오차 허용
  }

  @Test
  @DisplayName("동일한 두 지점 간의 거리 계산 테스트")
  void distanceKmSamePointTest() {
    double lat = 37.5665;
    double lng = 126.9780;

    double distance = GeoUtil.distanceKm(lat, lng, lat, lng);
    assertThat(distance).isCloseTo(0.0, org.assertj.core.api.Assertions.within(DELTA));
  }

  @Test
  @DisplayName("적도 상의 두 지점 간의 거리 계산 테스트")
  void distanceKmEquatorTest() {
    // 적도 상에서 경도만 다른 두 지점 (0, 0)과 (0, 90)
    double lat1 = 0.0;
    double lng1 = 0.0;
    double lat2 = 0.0;
    double lng2 = 90.0;
    double expectedDistance = 10007.5; // 지구 둘레의 1/4 (대략)

    double distance = GeoUtil.distanceKm(lat1, lng1, lat2, lng2);
    assertThat(distance).isCloseTo(expectedDistance, org.assertj.core.api.Assertions.within(DELTA * 100));
  }

  @Test
  @DisplayName("극점에서의 거리 계산 테스트")
  void distanceKmPoleTest() {
    // 북극 (90, 0)과 적도 (0, 0)
    double lat1 = 90.0;
    double lng1 = 0.0;
    double lat2 = 0.0;
    double lng2 = 0.0;
    double expectedDistance = 10007.5; // 북극에서 적도까지의 거리 (대략)

    double distance = GeoUtil.distanceKm(lat1, lng1, lat2, lng2);
    assertThat(distance).isCloseTo(expectedDistance, org.assertj.core.api.Assertions.within(DELTA * 100));
  }
}