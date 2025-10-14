package com.deliveranything.domain.notification.service;

import com.deliveranything.domain.notification.entity.Notification;
import com.deliveranything.domain.notification.enums.NotificationType;
import com.deliveranything.domain.notification.repository.EmitterRepository;
import com.deliveranything.domain.notification.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final EmitterRepository emitterRepository;

  // 알림 생성 및 전송 (모든 디바이스에 브로드캐스트)
  public Notification sendNotification(Long profileId, NotificationType type, String message, String data) {
    Notification notification = Notification.builder()
        .recipientId(profileId)
        .type(type)
        .message(message)
        .data(data)
        .build();

    notificationRepository.save(notification);
    broadcastToEmitters(profileId, notification, "notification");

    return notification;
  }

  // 알림 읽음 처리 및 다른 디바이스에 동기화
  @Transactional
  public void markAsRead(Long notificationId, Long profileId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

    if (!notification.getRecipientId().equals(profileId)) {
      throw new IllegalArgumentException("Invalid recipient for this notification");
    }

    if (!notification.isRead()) {
      notification.setRead(true);
      broadcastToEmitters(profileId, notificationId, "notification-read");
    }
  }

  // 알림 목록 조회
  public List<Notification> getNotifications(Long profileId, Boolean isRead) {
    return (isRead == null)
        ? notificationRepository.findByRecipientIdOrderByCreatedAtDesc(profileId)
        : notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(profileId, isRead);
  }

  // 읽지 않은 알림 개수 조회
  public long getUnreadCount(Long profileId) {
    return notificationRepository.countByRecipientIdAndIsReadFalse(profileId);
  }

  // 프로필 ID 기준 모든 SSE Emitter에 브로드캐스트
  private void broadcastToEmitters(Long profileId, Object payload, String eventName) {
    List<SseEmitter> emitters = emitterRepository.getAllForProfile(profileId);

    for (SseEmitter emitter : emitters) {
      try {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .id(resolveEventId(payload))
            .name(eventName)
            .data(payload);

        emitter.send(event);

      } catch (Exception e) {
        log.warn("SSE send failed for profileId {}: {}. Completing emitter.", profileId, e.getMessage());
        emitter.complete(); // onCompletion 콜백 유도
      }
    }
  }

  // payload에 따른 SSE 이벤트 ID 생성
  private String resolveEventId(Object payload) {
    if (payload instanceof Notification notification) {
      return notification.getId().toString();
    }
    return payload.toString();
  }
}