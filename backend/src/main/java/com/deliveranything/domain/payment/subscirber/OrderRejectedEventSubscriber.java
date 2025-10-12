package com.deliveranything.domain.payment.subscirber;

import com.deliveranything.domain.order.event.OrderRejectedEvent;
import com.deliveranything.domain.payment.service.PaymentService;
import com.deliveranything.global.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRejectedEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final PaymentService paymentService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("order-rejected-event"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    OrderRejectedEvent event = null;
    try {
      event = objectMapper.readValue(new String(message.getBody()), OrderRejectedEvent.class);
      paymentService.cancelPayment(event.merchantUid(), event.cancelReason(), event.publisher());
    } catch (CustomException e) {
      if (event != null) {
        log.warn("Payment cancel failed for order {}: {}", event.orderId(), e.getMessage());
      } else {
        log.error("Failed to get payment request message from Redis", e);
      }
    } catch (Exception e) {
      log.error("Failed to process order rejected event from Redis", e);
    }
  }
}
