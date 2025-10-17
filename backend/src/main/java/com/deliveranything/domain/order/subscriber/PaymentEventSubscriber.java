package com.deliveranything.domain.order.subscriber;

import com.deliveranything.domain.order.handler.PaymentEventHandler;
import com.deliveranything.global.enums.RedisTopic;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final PaymentEventHandler paymentEventHandler;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new PatternTopic(RedisTopic.PAYMENT_EVENT.getPattern()));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    String topic = new String(message.getChannel());
    String json = new String(message.getBody());
    log.debug("Received Redis event topic={}, body={}", topic, json);
    paymentEventHandler.handle(topic, json);
  }
}
