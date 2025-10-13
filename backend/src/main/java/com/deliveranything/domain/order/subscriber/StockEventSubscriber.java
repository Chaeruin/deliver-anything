package com.deliveranything.domain.order.subscriber;

import com.deliveranything.domain.order.handler.StockEventHandler;
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
public class StockEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final StockEventHandler orderEventHandler;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("stock-reserved-event"));
    container.addMessageListener(this, new ChannelTopic("stock-reserve-failed-event"));
    container.addMessageListener(this, new ChannelTopic("stock-committed-event"));
    container.addMessageListener(this, new ChannelTopic("stock-released-event"));
    container.addMessageListener(this, new ChannelTopic("stock-replenished-event"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    String topic = new String(pattern);
    String json = new String(message.getBody());
    log.debug("Received Redis event topic={}, body={}", topic, json);
    orderEventHandler.handle(topic, json);
  }
}