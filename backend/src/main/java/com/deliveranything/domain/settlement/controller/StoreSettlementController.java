package com.deliveranything.domain.settlement.controller;

import com.deliveranything.domain.settlement.dto.SettlementResponse;
import com.deliveranything.domain.settlement.service.SettlementBatchService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/store/settlements")
@RestController
@Tag(name = "상점 정산 API", description = "상점의 정산 조회 관련 API입니다.")
public class StoreSettlementController {

  private final SettlementBatchService settlementBatchService;

  @GetMapping("/{storeId}/day")
  @Operation(summary = "정산 일별 조회", description = "상점이 일별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getDaySettlements(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 일별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByDay(securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/{storeId}/week")
  @Operation(summary = "정산 주간별 조회", description = "상점이 주간별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getWeekSettlements(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 주간별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByWeek(securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/{storeId}/month")
  @Operation(summary = "정산 월별 조회", description = "상점이 월별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getMonthSettlements(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 월별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByMonth(
            securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/{storeId}/period")
  @Operation(summary = "정산 기간 조회", description = "상점이 특정 기간의 정산 정보를 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<SettlementResponse>> getPeriodSettlements(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("정산 기간 조회 성공",
        settlementBatchService.getSettlementByPeriod(securityUser.getCurrentActiveProfileIdSafe(),
            startDate, endDate)));
  }
}
