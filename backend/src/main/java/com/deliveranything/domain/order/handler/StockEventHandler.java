package com.deliveranything.domain.order.handler;

import com.deliveranything.domain.order.service.OrderService;
import com.deliveranything.domain.product.stock.event.StockCommittedEvent;
import com.deliveranything.domain.product.stock.event.StockReleasedEvent;
import com.deliveranything.domain.product.stock.event.StockReplenishedEvent;
import com.deliveranything.domain.product.stock.event.StockReserveFailedEvent;
import com.deliveranything.domain.product.stock.event.StockReservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventHandler {

  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  public void handle(String topic, String json) {
    try {
      switch (topic) {
        case "stock-reserved-event" -> {
          StockReservedEvent event = objectMapper.readValue(json, StockReservedEvent.class);
          orderService.processStockReserved(event.orderId());
        }
        case "stock-reserve-failed-event" -> {
          StockReserveFailedEvent event = objectMapper.readValue(json,
              StockReserveFailedEvent.class);
          orderService.processStockReserveFailed(event.orderId(), event.reason());
        }
        case "stock-committed-event" -> {
          StockCommittedEvent event = objectMapper.readValue(json, StockCommittedEvent.class);
          orderService.processStockCommitted(event.orderId());
        }
        case "stock-released-event" -> {
          StockReleasedEvent event = objectMapper.readValue(json, StockReleasedEvent.class);
          orderService.processStockReleased(event.orderId());
        }
        case "stock-replenished-event" -> {
          StockReplenishedEvent event = objectMapper.readValue(json, StockReplenishedEvent.class);
          orderService.processStockReplenished(event.orderId());
        }
        default -> log.warn("Unknown topic: {}", topic);
      }
    } catch (Exception e) {
      log.error("Failed to process order event [{}]: {}", topic, e.getMessage(), e);
    }
  }
}
