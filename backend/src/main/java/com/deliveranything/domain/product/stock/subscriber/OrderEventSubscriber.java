package com.deliveranything.domain.product.stock.subscriber;

import com.deliveranything.domain.product.stock.handler.OrderEventHandler;
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
public class OrderEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final OrderEventHandler orderEventHandler;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new PatternTopic("order-created-event"));
    container.addMessageListener(this, new PatternTopic("order-cancel-succeeded-event"));
    container.addMessageListener(this, new PatternTopic("order-payment-succeeded-event"));
    container.addMessageListener(this, new PatternTopic("order-payment-failed-event"));
  }
  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    String topic = new String(pattern);
    String json = new String(message.getBody());
    log.debug("Received Redis event topic={}, body={}", topic, json);
    orderEventHandler.handle(topic, json);
  }
}