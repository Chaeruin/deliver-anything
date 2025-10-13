package com.deliveranything.domain.product.stock.dto;

import com.deliveranything.domain.product.stock.entity.Stock;

public record StockResponse(
    Long productId,
    Integer quantity
) {
  public static StockResponse from(Stock stock) {
    return new StockResponse(
        stock.getProduct().getId(),
        stock.getTotalQuantity()
    );
  }
}