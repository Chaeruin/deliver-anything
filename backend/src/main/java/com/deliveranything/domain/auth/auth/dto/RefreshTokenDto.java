package com.deliveranything.domain.auth.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDto implements Serializable {

  private Long userId;
  private String tokenValue;
  private String deviceInfo;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;

  /**
   * Redis Key 생성 헬퍼
   */
  public static String generateKey(Long userId, String deviceInfo) {
    return String.format("refresh_token:%d:%s", userId, deviceInfo);
  }

  /**
   * 토큰 만료 여부
   */
  @JsonIgnore  // Redis 직렬화에서 제외
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  /**
   * 토큰 유효성
   */
  @JsonIgnore  // Redis 직렬화에서 제외
  public boolean isValid() {
    return !isExpired();
  }
}