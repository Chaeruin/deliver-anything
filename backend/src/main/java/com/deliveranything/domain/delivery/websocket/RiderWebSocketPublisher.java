package com.deliveranything.domain.delivery.websocket;

import com.deliveranything.domain.delivery.dto.RiderLocationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiderWebSocketPublisher {

  private final SimpMessagingTemplate messagingTemplate;

  // 여기 SSE 변경 보류
//  public void publishToRider(String riderId, RiderNotificationDto dto) {
//    String destination = "/topic/rider/" + riderId;
//    messagingTemplate.convertAndSend(destination, dto);
//  }

  public void publishLocation(Long riderProfileId, RiderLocationDto location) {
    String destination = "/topic/rider/location/" + riderProfileId;
    messagingTemplate.convertAndSend(destination, location);
  }
}
