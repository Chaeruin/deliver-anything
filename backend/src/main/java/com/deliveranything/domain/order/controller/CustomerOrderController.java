package com.deliveranything.domain.order.controller;

import static org.springframework.http.HttpStatus.CREATED;

import com.deliveranything.domain.order.dto.OrderCancelRequest;
import com.deliveranything.domain.order.dto.OrderCreateRequest;
import com.deliveranything.domain.order.dto.OrderPayRequest;
import com.deliveranything.domain.order.dto.OrderResponse;
import com.deliveranything.domain.order.service.CustomerOrderService;
import com.deliveranything.domain.order.service.PaymentOrderService;
import com.deliveranything.global.common.ApiResponse;
import com.deliveranything.global.common.CursorPageResponse;
import com.deliveranything.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/orders")
@RestController
@Tag(name = "고객 주문 API", description = "소비자의 주문처리 관련 API입니다.")
public class CustomerOrderController {

  private final CustomerOrderService customerOrderService;
  private final PaymentOrderService paymentOrderService;

  @PostMapping
  @Operation(summary = "주문 생성", description = "소비자가 상점에 주문을 요청한 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<OrderResponse>> create(
      @AuthenticationPrincipal SecurityUser securityUser,
      @Valid @RequestBody OrderCreateRequest orderCreateRequest
  ) {
    return ResponseEntity.status(CREATED).body(ApiResponse.success(customerOrderService.createOrder(
        securityUser.getCurrentActiveProfile().getId(), orderCreateRequest)));
  }

  @GetMapping
  @Operation(summary = "주문 내역 조회", description = "소비자가 주문 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<CursorPageResponse<OrderResponse>>> getAll(
      @AuthenticationPrincipal SecurityUser securityUser,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("소비자 전체 주문 내역 조회 성공",
        customerOrderService.getCustomerOrdersByCursor(securityUser.getId(), cursor, size)));
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "주문 단일 조회", description = "소비자가 어떤 주문의 상세 정보를 요청한 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<OrderResponse>> get(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long orderId
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("소비자 주문 단일 조회 성공",
        customerOrderService.getCustomerOrder(orderId, securityUser.getId())));
  }

  @GetMapping("/in-progress")
  @Operation(summary = "진행중인 주문 조회", description = "소비자가 진행중인 주문 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<List<OrderResponse>>> getInProgressOrders(
      @AuthenticationPrincipal SecurityUser securityUser
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("진행중인 소비자 주문 조회 성공",
        customerOrderService.getProgressingOrders(securityUser.getId())));
  }

  @GetMapping("/completed")
  @Operation(summary = "배달 완료된 주문 조회", description = "소비자가 배달 완료된 주문 내역을 요청한 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<CursorPageResponse<OrderResponse>>> getCompletedOrders(
      @AuthenticationPrincipal SecurityUser securityUser,
      @RequestParam(required = false) String nextPageToken,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("배달 완료된 소비자 주문 조회 성공",
        customerOrderService.getCompletedOrdersByCursor(securityUser.getId(), nextPageToken,
            size)));
  }

  @PostMapping("/{merchantUid}/pay")
  @Operation(summary = "주문 결제", description = "소비자가 생성한 주문의 결제 시도")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<OrderResponse>> pay(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable String merchantUid,
      @Valid @RequestBody OrderPayRequest orderPayRequest
  ) {

    return ResponseEntity.ok().body(ApiResponse.success("소비자 결제 승인 성공",
        paymentOrderService.payOrder(merchantUid, orderPayRequest.paymentKey())));
  }

  @PostMapping("/{orderId}/cancel")
  @Operation(summary = "주문 취소", description = "소비자가 상점에서 주문 수락 전인 주문을 취소하는 경우")
  @PreAuthorize("@profileSecurity.isCustomer(#securityUser)")
  public ResponseEntity<ApiResponse<OrderResponse>> cancel(
      @AuthenticationPrincipal SecurityUser securityUser,
      @PathVariable Long orderId,
      @RequestBody OrderCancelRequest orderCancelRequest
  ) {
    return ResponseEntity.ok().body(ApiResponse.success("소비자 주문 취소 성공",
        paymentOrderService.cancelOrder(orderId, orderCancelRequest.cancelReason())));
  }
}