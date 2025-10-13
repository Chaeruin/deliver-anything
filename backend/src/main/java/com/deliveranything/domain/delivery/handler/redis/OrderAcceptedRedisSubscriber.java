package com.deliveranything.domain.delivery.handler.redis;

import com.deliveranything.domain.delivery.event.dto.RiderNotificationDto;
import com.deliveranything.domain.delivery.service.OrderNotificationService;
import com.deliveranything.domain.notification.subscriber.delivery.OrderAcceptedNotifier;
import com.deliveranything.domain.order.event.OrderAcceptedEvent;
import com.deliveranything.global.exception.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Virtual Thread 기반 Redis Pub/Sub Subscriber - 주문 접수 이벤트를 수신하여 반경 내 라이더에게 알림 전송 - MessageListener를
 * 구현하여 블로킹 방식으로 처리 - RedisMessageListenerContainer의 TaskExecutor가 Virtual Thread를 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAcceptedRedisSubscriber implements MessageListener {

  public static final String CHANNEL = "order-accepted-event";

  private final ObjectMapper objectMapper;
  private final OrderAcceptedNotifier orderAcceptedNotifier;
  private final OrderNotificationService orderNotificationService;
  private final RedisMessageListenerContainer container;

  @PostConstruct
  public void subscribe() {
    container.addMessageListener(this, new ChannelTopic(CHANNEL));
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      String payload = new String(message.getBody());
      OrderAcceptedEvent event = objectMapper.readValue(payload, OrderAcceptedEvent.class);

      List<RiderNotificationDto> notifications = orderNotificationService.processOrderEvent(event);
      if (!notifications.isEmpty()) {
        orderAcceptedNotifier.publish(notifications);
      } else {
        log.warn("No available riders for orderId: {} (This is not an error)", event.orderId());
      }

    } catch (JsonProcessingException e) {
      log.error("Failed to parse order event: {}", e.getMessage());
    } catch (CustomException e) {
      log.error("Business error processing order event: {} - {}", e.getCode(), e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error processing order event: {}", e.getMessage(), e);
    }
    // 예외를 throw하지 않음 → Redis 연결 유지
  }
}
