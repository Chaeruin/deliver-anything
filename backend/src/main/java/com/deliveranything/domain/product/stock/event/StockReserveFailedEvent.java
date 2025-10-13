package com.deliveranything.domain.product.stock.event;

public record StockReserveFailedEvent(Long orderId, String reason) {

}