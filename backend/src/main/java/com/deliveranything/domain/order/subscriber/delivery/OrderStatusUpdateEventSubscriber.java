package com.deliveranything.domain.order.subscriber.delivery;

import com.deliveranything.domain.delivery.enums.DeliveryStatus;
import com.deliveranything.domain.delivery.event.dto.OrderStatusUpdateEvent;
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
public class OrderStatusUpdateEventSubscriber implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final OrderService orderService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("order-delivery-status"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    try {
      OrderStatusUpdateEvent event = objectMapper.readValue(message.getBody(),
          OrderStatusUpdateEvent.class);
      if (event.status() == DeliveryStatus.RIDER_ASSIGNED) {
        orderService.processDeliveryRiderAssigned(Long.parseLong(event.orderId()));
      }
    } catch (Exception e) {
      log.error("Failed to process order delivery status event from Redis", e);
    }
  }
}
