//package com.deliveranything.domain.product.stock.controller;
//
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@Tag(name = "상품 재고 관련 API", description = "상품 재고 관련 API입니다.")
//@RestController
//@RequestMapping("/api/v1/stores/{storeId}/products/{productId}/stock")
//@RequiredArgsConstructor
//public class StockController {
//
//  private final StockService stockService;
//
//  @Operation(summary = "상품 재고 조회", description = "특정 상품의 재고 정보를 조회합니다.")
//  @GetMapping
//  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId, #securityUser)")
//  public ResponseEntity<ApiResponse<StockResponse>> getProductStock(
//      @Parameter(description = "상점 ID") @PathVariable Long storeId,
//      @Parameter(description = "재고를 조회할 상품 ID") @PathVariable Long productId,
//      @AuthenticationPrincipal SecurityUser securityUser
//  ) {
//    return ResponseEntity.ok(ApiResponse.success(stockService.getProductStock(storeId, productId)));
//  }
//
//  @Operation(summary = "상품 재고 수정", description = "특정 상품의 재고를 수정합니다.")
//  @PutMapping
//  @PreAuthorize("@profileSecurity.isSeller(#securityUser) and @storeSecurity.isOwner(#storeId, #securityUser)")
//  public ResponseEntity<ApiResponse<StockResponse>> updateProductStock(
//      @Parameter(description = "상점 ID") @PathVariable Long storeId,
//      @Parameter(description = "재고를 수정할 상품 ID") @PathVariable Long productId,
//      @Valid @RequestBody StockUpdateRequest request,
//      @AuthenticationPrincipal SecurityUser securityUser
//  ) {
//    return ResponseEntity.ok(ApiResponse.success(stockService.updateStockByAdmin(storeId, productId, request.stockChange())));
//  }
//}