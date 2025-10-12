package com.deliveranything.domain.notification.subscriber.seller;

import com.deliveranything.domain.notification.enums.NotificationMessage;
import com.deliveranything.domain.notification.enums.NotificationType;
import com.deliveranything.domain.notification.service.NotificationService;
import com.deliveranything.domain.order.event.sse.seller.OrderCanceledForSellerEvent;
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
public class OrderCancelFailedSellerNotifier implements MessageListener {

  private final RedisMessageListenerContainer container;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @PostConstruct
  public void registerListener() {
    container.addMessageListener(this, new ChannelTopic("order-cancel-failed-for-seller-event"));
  }

  @Override
  public void onMessage(@NonNull Message message, byte[] pattern) {
    try {
      OrderCanceledForSellerEvent event = objectMapper.readValue(message.getBody(),
          OrderCanceledForSellerEvent.class);
      notificationService.sendNotification(
          event.sellerId(),
          NotificationType.ORDER_CANCEL_FAILED_SELLER,
          NotificationMessage.ORDER_CANCEL_FAILED_SELLER.getMessage(),
          objectMapper.writeValueAsString(event)
      );
    } catch (Exception e) {
      log.error("Failed to process order cancel failed for seller event from Redis", e);
    }
  }
}
