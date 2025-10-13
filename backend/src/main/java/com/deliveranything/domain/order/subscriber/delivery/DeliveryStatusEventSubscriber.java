package com.deliveranything.domain.order.subscriber.delivery;

import com.deliveranything.domain.delivery.enums.DeliveryStatus;
import com.deliveranything.domain.delivery.event.dto.DeliveryStatusEvent;
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
public class DeliveryStatusEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("delivery-status-events"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    try {
      DeliveryStatusEvent event = objectMapper.readValue(message.getBody(),
          DeliveryStatusEvent.class);
      if (event.status() == DeliveryStatus.PICKED_UP) {
        orderService.processDeliveryPickedUp(event.orderId());
      } else if (event.status() == DeliveryStatus.COMPLETED) {
        orderService.processDeliveryCompleted(event.orderId(), event.riderProfileId(),
            event.sellerProfileId());
      }
    } catch (Exception e) {
      log.error("Failed to process delivery status event from Redis", e);
    }
  }
}
