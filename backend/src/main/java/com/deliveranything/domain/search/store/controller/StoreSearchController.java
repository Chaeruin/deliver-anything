package com.deliveranything.domain.search.store.controller;

import com.deliveranything.domain.search.store.dto.StoreSearchRequest;
import com.deliveranything.domain.search.store.dto.StoreSearchResponse;
import com.deliveranything.domain.search.store.service.StoreSearchService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상점 검색 API", description = "상점 검색 관련 API입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search/stores")
public class StoreSearchController {

  private final StoreSearchService storeSearchService;

  @Operation(summary = "상점 검색", description = "상점 검색 결과를 커서 기반 페이지네이션으로 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<CursorPageResponse<StoreSearchResponse>>> searchStores(
      @Valid @ModelAttribute StoreSearchRequest request
  ) {
    CursorPageResponse<StoreSearchResponse> results = storeSearchService.search(request);

    return ResponseEntity.ok(ApiResponse.success(results));
  }
}