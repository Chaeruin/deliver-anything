package com.deliveranything.domain.product.stock.event;

import com.deliveranything.domain.order.event.dto.OrderItemInfo;
import java.util.List;

public record StockReservedFailedEvent(
    Long orderId,
    Long storeId,
    List<OrderItemInfo> items,
    String reason
) {

}