package com.deliveranything.domain.settlement.controller;

import com.deliveranything.domain.settlement.dto.SettlementResponse;
import com.deliveranything.domain.settlement.dto.SummaryResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/rider/settlements")
@RestController
@Tag(name = "배달원 정산 API", description = "라이더의 정산 조회 관련 API입니다.")
public class RiderSettlementController {

  private final SettlementBatchService settlementBatchService;

  @GetMapping("/day")
  @Operation(summary = "정산 일별 조회", description = "배달원이 일별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isRider(#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getDaySettlements(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("배달원의 일별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByDay(securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/week")
  @Operation(summary = "정산 주간별 조회", description = "배달원이 주간별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isRider(#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getWeekSettlements(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("배달원의 주간별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByWeek(securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/month")
  @Operation(summary = "정산 월별 조회", description = "배달원이 월별 정산 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isRider(#securityUser)")
  public ResponseEntity<ApiResponse<List<SettlementResponse>>> getMonthSettlements(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("배달원의 월별 정산 내역 조회 성공",
        settlementBatchService.getSettlementsByMonth(
            securityUser.getCurrentActiveProfileIdSafe())));
  }

  @GetMapping("/period")
  @Operation(summary = "정산 기간 조회", description = "배달원이 특정 기간의 정산 정보를 요청한 경우")
  @PreAuthorize("@profileSecurity.isRider(#securityUser)")
  public ResponseEntity<ApiResponse<SettlementResponse>> getPeriodSettlements(
      @AuthenticationPrincipal SecurityUser securityUser,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("정산 기간 조회 성공",
        settlementBatchService.getSettlementByPeriod(securityUser.getCurrentActiveProfileIdSafe(),
            startDate, endDate)));
  }

  @GetMapping("/summary")
  @Operation(summary = "정산 요약 조회", description = "배달원이 정산 요약 카드 정보를 요청한 경우")
  @PreAuthorize("@profileSecurity.isRider(#securityUser)")
  public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("정산 요약 카드 조회 성공",
        settlementBatchService.getSettlementSummary(securityUser.getCurrentActiveProfileIdSafe())));
  }
}
