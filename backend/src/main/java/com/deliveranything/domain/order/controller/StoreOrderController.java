package com.deliveranything.domain.order.controller;

import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.service.StoreOrderService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/stores/{storeId}/orders")
@RestController
@Tag(name = "상점 주문 API", description = "판매자의 주문처리 관련 API입니다.")
public class StoreOrderController {

  private final StoreOrderService storeOrderService;

  @GetMapping("/history")
  @Operation(summary = "주문 내역 조회", description = "판매자가 주문 이력을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<CursorPageResponse<OrderResponse>>> getOrdersHistory(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId,
      @RequestParam(required = false) String nextPageToken,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 전체 주문 내역 조회 성공",
        storeOrderService.getStoreOrdersByCursor(storeId, nextPageToken, size)));
  }

  @GetMapping("/pending")
  @Operation(summary = "주문 수락 대기 목록 조회", description = "판매자가 상점의 주문 수락 대기 목록을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 주문 수락 대기 목록 조회 성공",
        storeOrderService.getPendingOrders(storeId)));
  }

  @GetMapping("/accepted")
  @Operation(summary = "주문 현황 목록 조회", description = "판매자가 상점의 주문 처리 중인 목록을 요청한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<List<OrderResponse>>> getAcceptedOrders(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("상점의 주문 현황 목록 조회 성공",
        storeOrderService.getAcceptedOrders(storeId)));
  }

  @PatchMapping("/{orderId}/accept")
  @Operation(summary = "주문 수락", description = "판매자가 상점의 주문을 수락한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<String>> acceptOrder(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId,
      @PathVariable Long orderId
  ) {
    storeOrderService.acceptOrder(orderId);
    return ResponseEntity.ok().body(ApiResponse.success("주문 수락 요청 처리중입니다."));
  }

  @PatchMapping("/{orderId}/reject")
  @Operation(summary = "주문 거절", description = "판매자가 상점의 주문을 거절한 경우")
  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId,#securityUser)")
  public ResponseEntity<ApiResponse<String>> rejectOrder(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long storeId,
      @PathVariable Long orderId
  ) {
    storeOrderService.rejectOrder(orderId);
    return ResponseEntity.ok().body(ApiResponse.success("주문 거절 요청 처리중입니다."));
  }
}
