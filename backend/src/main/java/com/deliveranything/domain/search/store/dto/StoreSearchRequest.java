package com.deliveranything.domain.search.store.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StoreSearchRequest(
    @Parameter(description = "현재 위도", example = "37.5665")
    @NotNull(message = "위도(lat)는 필수 입력 값입니다.")
    Double lat,

    @Parameter(description = "현재 경도", example = "126.9780")
    @NotNull(message = "경도(lng)는 필수 입력 값입니다.")
    Double lng,

    @Parameter(description = "카테고리 ID (선택 사항)", example = "1")
    Long categoryId,

    @Parameter(description = "검색어 (선택 사항)", example = "치킨")
    @Size(max = 50, message = "검색어는 50자를 초과할 수 없습니다.")
    String searchText,

    @Parameter(description = "검색 반경 (km, 기본값 7km)", example = "5.0")
    @Min(value = 0, message = "거리는 0km 이상이어야 합니다.")
    @Max(value = 10, message = "거리는 10km를 초과할 수 없습니다.")
    Double distanceKm,

    @Parameter(description = "페이지당 결과 수 (기본값 20)", example = "10")
    @Min(value = 1, message = "limit 값은 1 이상이어야 합니다.")
    @Max(value = 50, message = "limit 값은 50을 초과할 수 없습니다.")
    Integer limit,

    @Parameter(description = "다음 페이지 토큰 (이전 응답에서 받은 값)", example = "eyJpZCI6MSwiZGlzdGFuY2UiOjEuNX0=")
    String nextPageToken
) {
  public StoreSearchRequest {
    if (distanceKm == null) { distanceKm = 7.0; }
    if (limit == null) { limit = 20; }
  }
}