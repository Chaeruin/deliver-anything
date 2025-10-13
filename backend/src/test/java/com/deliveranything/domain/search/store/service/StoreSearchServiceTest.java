package com.deliveranything.domain.search.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

import com.deliveranything.domain.search.store.document.StoreDocument;
import com.deliveranything.domain.search.store.dto.StoreSearchRequest;
import com.deliveranything.domain.search.store.dto.StoreSearchResponse;
import com.deliveranything.domain.search.store.repository.StoreSearchRepository;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.util.GeoUtil;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreSearchServiceTest {

  @Mock
  private StoreSearchRepository storeSearchRepository;

  @InjectMocks
  private StoreSearchService storeSearchService;

  private StoreSearchRequest searchRequest;
  private StoreDocument storeDocument;

  @BeforeEach
  void setUp() {
    searchRequest = new StoreSearchRequest(
        37.5665,
        126.9780,
        1L,
        "",
        10.0,
        null,
        null
    );

    storeDocument = StoreDocument.builder()
        .id(1L)
        .name("Test Store")
        .roadAddress("Test Address")
        .status(com.deliveranything.domain.store.store.enums.StoreStatus.OPEN)
        .imageUrl("http://example.com/image.jpg")
        .categoryName("Korean Food")
        .location(new org.springframework.data.elasticsearch.core.geo.GeoPoint(37.5670, 126.9790))
        .build();
  }

  @Test
  @DisplayName("상점 검색 성공 테스트")
  void searchStoresSuccessTest() {
    try (MockedStatic<GeoUtil> mockedGeoUtil = Mockito.mockStatic(GeoUtil.class)) {
      mockedGeoUtil.when(() -> GeoUtil.distanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .thenReturn(1.5); // 1.5 km

      CursorPageResponse<StoreDocument> mockResponse = new CursorPageResponse<>(
          List.of(storeDocument), "nextCursor", true
      );
      when(storeSearchRepository.search(searchRequest)).thenReturn(mockResponse);

      CursorPageResponse<StoreSearchResponse> result = storeSearchService.search(searchRequest);

      assertThat(result).isNotNull();
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().getFirst().id()).isEqualTo(storeDocument.getId());
      assertThat(result.content().getFirst().distance()).isEqualTo(1.5);
      assertThat(result.content().getFirst().deliveryFee()).isEqualTo(3000); // 1.5km -> 3000원
      assertThat(result.nextPageToken()).isEqualTo("nextCursor");
      assertThat(result.hasNext()).isTrue();
    }
  }

  @Test
  @DisplayName("상점 검색 결과 없음 테스트")
  void searchStoresNoResultTest() {
    CursorPageResponse<StoreDocument> mockResponse = new CursorPageResponse<>(
        Collections.emptyList(), null, false
    );
    when(storeSearchRepository.search(searchRequest)).thenReturn(mockResponse);

    CursorPageResponse<StoreSearchResponse> result = storeSearchService.search(searchRequest);

    assertThat(result).isNotNull();
    assertThat(result.content()).isEmpty();
    assertThat(result.nextPageToken()).isNull();
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  @DisplayName("배달 요금 계산 테스트 - 기본 거리 이내")
  void estimateDeliveryFeeBaseDistanceTest() {
    // private 메서드 테스트를 위해 리플렉션 사용 또는 public 메서드를 통해 간접 테스트
    // 여기서는 public search 메서드를 통해 간접 테스트
    try (MockedStatic<GeoUtil> mockedGeoUtil = Mockito.mockStatic(GeoUtil.class)) {
      mockedGeoUtil.when(() -> GeoUtil.distanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .thenReturn(2.0); // 2.0 km

      CursorPageResponse<StoreDocument> mockResponse = new CursorPageResponse<>(
          List.of(storeDocument), null, false
      );
      when(storeSearchRepository.search(searchRequest)).thenReturn(mockResponse);

      CursorPageResponse<StoreSearchResponse> result = storeSearchService.search(searchRequest);
      assertThat(result.content().getFirst().deliveryFee()).isEqualTo(3000);
    }
  }

  @Test
  @DisplayName("배달 요금 계산 테스트 - 기본 거리 초과")
  void estimateDeliveryFeeExtraDistanceTest() {
    try (MockedStatic<GeoUtil> mockedGeoUtil = Mockito.mockStatic(GeoUtil.class)) {
      mockedGeoUtil.when(() -> GeoUtil.distanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .thenReturn(4.5); // 4.5 km

      CursorPageResponse<StoreDocument> mockResponse = new CursorPageResponse<>(
          List.of(storeDocument), null, false
      );
      when(storeSearchRepository.search(searchRequest)).thenReturn(mockResponse);

      CursorPageResponse<StoreSearchResponse> result = storeSearchService.search(searchRequest);
      // 3km 기본 3000원, 1.5km 초과 -> 올림하여 2km 추가 -> 2000원 추가
      assertThat(result.content().getFirst().deliveryFee()).isEqualTo(3000 + 2 * 1000);
    }
  }

  @Test
  @DisplayName("배달 요금 계산 테스트 - 정확히 기본 거리")
  void estimateDeliveryFeeExactBaseDistanceTest() {
    try (MockedStatic<GeoUtil> mockedGeoUtil = Mockito.mockStatic(GeoUtil.class)) {
      mockedGeoUtil.when(() -> GeoUtil.distanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .thenReturn(3.0); // 3.0 km

      CursorPageResponse<StoreDocument> mockResponse = new CursorPageResponse<>(
          List.of(storeDocument), null, false
      );
      when(storeSearchRepository.search(searchRequest)).thenReturn(mockResponse);

      CursorPageResponse<StoreSearchResponse> result = storeSearchService.search(searchRequest);
      assertThat(result.content().getFirst().deliveryFee()).isEqualTo(3000);
    }
  }
}