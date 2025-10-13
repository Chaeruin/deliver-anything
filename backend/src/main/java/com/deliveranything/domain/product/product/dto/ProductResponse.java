package com.deliveranything.domain.product.product.dto;

import com.deliveranything.domain.product.product.entity.Product;

public record ProductResponse(
    Long productId,
    Long storeId,
    String name,
    Integer price,
    Integer availableQuantity,
    String imageUrl
) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getStore().getId(),
        product.getName(),
        product.getPrice(),
        product.getStock().getAvailableQuantity(),
        product.getImageUrl()
    );
  }
}