package com.deliveranything.domain.order.handler;

import com.deliveranything.domain.order.service.OrderService;
import com.deliveranything.domain.payment.event.PaymentCancelFailedEvent;
import com.deliveranything.domain.payment.event.PaymentCancelSuccessEvent;
import com.deliveranything.domain.payment.event.PaymentFailedEvent;
import com.deliveranything.domain.payment.event.PaymentSuccessEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  public void handle(String topic, String json) {
    try {
      switch (topic) {
        case "payment-completed-event" -> {
          PaymentSuccessEvent event = objectMapper.readValue(json, PaymentSuccessEvent.class);
          orderService.processPaymentCompletion(event.merchantUid());
        }
        case "payment-failed-event" -> {
          PaymentFailedEvent event = objectMapper.readValue(json, PaymentFailedEvent.class);
          orderService.processPaymentFailure(event.merchantUid());
        }
        case "payment-cancel-success-event" -> {
          PaymentCancelSuccessEvent event = objectMapper.readValue(json,
              PaymentCancelSuccessEvent.class);
          orderService.processPaymentCancelSuccess(event.merchantUid(), event.publisher());
        }
        case "payment-cancel-failed-event" -> {
          PaymentCancelFailedEvent event = objectMapper.readValue(json,
              PaymentCancelFailedEvent.class);
          orderService.processPaymentCancelFailed(event.merchantId());
        }
        default -> log.warn("Unknown topic: {}", topic);
      }
    } catch (Exception e) {
      log.error("Failed to process payment event in order [{}]: {}", topic, e.getMessage(), e);
    }
  }
}
