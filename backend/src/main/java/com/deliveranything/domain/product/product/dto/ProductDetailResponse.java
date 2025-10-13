package com.deliveranything.domain.product.product.dto;

import com.deliveranything.domain.product.product.entity.Product;

public record ProductDetailResponse(
    Long productId,
    Long storeId,
    String name,
    String description,
    Integer price,
    Integer stockTotalQuantity,
    Integer availableQuantity,
    String imageUrl
) {

  public static ProductDetailResponse from(Product product) {
    return new ProductDetailResponse(
        product.getId(),
        product.getStore().getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getStock().getTotalQuantity(),
        product.getStock().getAvailableQuantity(),
        product.getImageUrl()
    );
  }
}