package com.deliveranything.domain.order.subscriber.payment;

import com.deliveranything.domain.order.service.OrderService;
import com.deliveranything.domain.payment.event.PaymentCancelFailedEvent;
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
public class PaymentCancelFailedEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("payment-cancel-success-event"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    try {
      orderService.processPaymentCancelFailed(objectMapper.readValue(message.getBody(),
          PaymentCancelFailedEvent.class).merchantId());
    } catch (Exception e) {
      log.error("Failed to process payment cancel success event from Redis", e);
    }
  }
}
