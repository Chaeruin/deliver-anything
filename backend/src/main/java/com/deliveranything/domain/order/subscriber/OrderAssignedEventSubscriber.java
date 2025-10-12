package com.deliveranything.domain.order.subscriber;

import com.deliveranything.domain.delivery.event.dto.OrderAssignedEvent;
import com.deliveranything.domain.order.service.OrderService;
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
public class OrderAssignedEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("order-assigned-event"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    try {
      OrderAssignedEvent event = objectMapper.readValue(message.getBody(),
          OrderAssignedEvent.class);
      orderService.processOrderTransmitted(event.orderId());
    } catch (Exception e) {
      log.error("Failed to process order assigned event from Redis", e);
    }
  }
}
