package com.deliveranything.domain.notification.entity;

import com.deliveranything.domain.notification.enums.NotificationType;
import com.deliveranything.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@Table(
    name = "notifications",
    indexes = @Index(name = "idx_notification_recipient_id_created_at", columnList = "recipientId, createdAt")
)
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

  @Column(nullable = false)
  private Long recipientId; // Profile ID

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  @Column(nullable = false, length = 255)
  private String message;

  @Column(length = 500)
  private String data; // JSON

  @Builder.Default
  @Column(nullable = false)
  private boolean isRead = false;

}