package com.deliveranything.domain.notification.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EmitterRepository {

  // 이중 맵 구조: profileId -> (deviceId -> SseEmitter)
  private final Map<Long, Map<String, SseEmitter>> profileEmitters = new ConcurrentHashMap<>();

  /**
   * Emitter 저장
   *
   * @param profileId 사용자 프로필 ID
   * @param deviceId  기기 ID
   * @param emitter   SseEmitter 객체
   */
  public void save(Long profileId, String deviceId, SseEmitter emitter) {
    // profileId에 해당하는 맵이 없으면 새로 생성
    profileEmitters.computeIfAbsent(profileId, k -> new ConcurrentHashMap<>())
        .put(deviceId, emitter);
  }

  /**
   * 특정 Emitter 제거
   *
   * @param profileId 사용자 프로필 ID
   * @param deviceId  기기 ID
   */
  public void remove(Long profileId, String deviceId) {
    Map<String, SseEmitter> deviceEmitters = profileEmitters.get(profileId);
    if (deviceEmitters != null) {
      deviceEmitters.remove(deviceId);
      // 해당 profileId에 더 이상 연결된 device가 없으면 바깥쪽 맵에서도 제거
      if (deviceEmitters.isEmpty()) {
        profileEmitters.remove(profileId);
      }
    }
  }

  /**
   * 특정 사용자의 모든 Emitter 조회 (알림 발송 시 사용)
   *
   * @param profileId 사용자 프로필 ID
   * @return 해당 사용자의 모든 SseEmitter 목록
   */
  public List<SseEmitter> getAllForProfile(Long profileId) {
    Map<String, SseEmitter> deviceEmitters = profileEmitters.get(profileId);
    if (deviceEmitters != null) {
      return new ArrayList<>(deviceEmitters.values());
    }
    return new ArrayList<>(); // 비어있는 리스트 반환
  }

  /**
   * 특정 profileId와 deviceId에 해당하는 Emitter 조회
   *
   * @param profileId 사용자 프로필 ID
   * @param deviceId  기기 ID
   * @return 해당 SseEmitter 객체 또는 null
   */
  public SseEmitter get(Long profileId, String deviceId) {
    Map<String, SseEmitter> deviceEmitters = profileEmitters.get(profileId);
    if (deviceEmitters != null) {
      return deviceEmitters.get(deviceId);
    }
    return null;
  }

  /**
   * 모든 Emitter 조회 (하트비트 발송 시 사용)
   *
   * @return 전체 SseEmitter 맵
   */
  public Map<Long, Map<String, SseEmitter>> getAllEmitters() {
    return new ConcurrentHashMap<>(profileEmitters);
  }

  /**
   * 특정 기기의 Emitter를 한 프로필에서 다른 프로필로 이전
   *
   * @param oldProfileId 이전 프로필 ID
   * @param newProfileId 새 프로필 ID
   * @param deviceId     기기 ID
   */
  public void move(Long oldProfileId, Long newProfileId, String deviceId) {
    Map<String, SseEmitter> oldDeviceEmitters = profileEmitters.get(oldProfileId);
    if (oldDeviceEmitters != null) {
      SseEmitter emitter = oldDeviceEmitters.remove(deviceId);
      if (emitter != null) {
        // 이전 프로필에 더 이상 연결된 기기가 없으면 맵에서 제거
        if (oldDeviceEmitters.isEmpty()) {
          profileEmitters.remove(oldProfileId);
        }
        // 새 프로필에 Emitter 저장
        save(newProfileId, deviceId, emitter);
      }
    }
  }

}