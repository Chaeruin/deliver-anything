package com.deliveranything.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;

class PointUtilTest {

  @Test
  @DisplayName("유효한 위도와 경도로 Point 객체 생성 테스트")
  void createPointWithValidCoordinatesTest() {
    double lat = 37.5665;
    double lng = 126.9780;

    Point point = PointUtil.createPoint(lat, lng);

    assertThat(point).isNotNull();
    assertThat(point.getX()).isEqualTo(lng); // JTS Point는 경도(X), 위도(Y) 순서
    assertThat(point.getY()).isEqualTo(lat);
    assertThat(point.getSRID()).isEqualTo(4326);
  }

  @Test
  @DisplayName("음수 위도와 경도로 Point 객체 생성 테스트")
  void createPointWithNegativeCoordinatesTest() {
    double lat = -30.0;
    double lng = -60.0;

    Point point = PointUtil.createPoint(lat, lng);

    assertThat(point).isNotNull();
    assertThat(point.getX()).isEqualTo(lng);
    assertThat(point.getY()).isEqualTo(lat);
  }

  @Test
  @DisplayName("경계값 위도와 경도로 Point 객체 생성 테스트")
  void createPointWithBoundaryCoordinatesTest() {
    double lat = 90.0; // 북극
    double lng = 180.0; // 날짜 변경선

    Point point = PointUtil.createPoint(lat, lng);

    assertThat(point).isNotNull();
    assertThat(point.getX()).isEqualTo(lng);
    assertThat(point.getY()).isEqualTo(lat);
  }
}