package com.deliveranything.domain.product.stock.handler;

import com.deliveranything.domain.order.event.*;
import com.deliveranything.domain.product.stock.service.StockFacadeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

  private final ObjectMapper objectMapper;
  private final StockFacadeService stockFacadeService;

  public void handle(String topic, String json) {
    try {
      switch (topic) {
        case "order-created-event" -> {
          var event = objectMapper.readValue(json, OrderCreatedEvent.class);
          stockFacadeService.handleOrderCreated(event.orderId(), event.storeId(), event.orderItems());
        }
        case "order-cancel-succeeded-event" -> {
          var event = objectMapper.readValue(json, OrderCancelSucceededEvent.class);
          stockFacadeService.handleOrderCancelSucceeded(event.orderId(), event.storeId(), event.orderItems());
        }
        case "order-payment-succeeded-event" -> {
          var event = objectMapper.readValue(json, OrderPaymentSucceededEvent.class);
          stockFacadeService.handleOrderPaymentSucceeded(event.orderId(), event.storeId(), event.orderItems());
        }
        case "order-payment-failed-event" -> {
          var event = objectMapper.readValue(json, OrderPaymentFailedEvent.class);
          stockFacadeService.handleOrderPaymentFailed(event.orderId(), event.storeId(), event.orderItems());
        }
        default -> log.warn("Unknown topic: {}", topic);
      }
    } catch (Exception e) {
      log.error("Failed to process order event [{}]: {}", topic, e.getMessage(), e);
    }
  }
}