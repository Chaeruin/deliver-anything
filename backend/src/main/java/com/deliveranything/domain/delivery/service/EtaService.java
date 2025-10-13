package com.deliveranything.domain.delivery.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Virtual Thread 기반 Kakao Map API 서비스 - WebClient.block()은 Virtual Thread에서 안전하게 사용 가능 - 블로킹 호출이지만
 * Virtual Thread 덕분에 높은 동시성 유지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtaService {

  private final WebClient.Builder webClientBuilder;

  @Value("${kakao.api.key}")
  private String kakaoApiKey;

  private static final String KAKAO_BASE_URL = "https://apis-navi.kakaomobility.com/v1";

  /**
   * 여러 라이더의 ETA 계산 (동기식 + 병렬 처리) - Virtual Thread에서 병렬로 실행 - @Async로 각 API 호출을 독립적인 Virtual
   * Thread에서 처리
   */
  public Map<String, Double> getEtaForMultiple(
      double userLat, double userLon,
      List<Point> riderPoints,
      List<String> riderIds
  ) {
    // 각 라이더의 ETA를 병렬로 계산 (Virtual Thread)
    List<CompletableFuture<Map.Entry<String, Double>>> futures = new java.util.ArrayList<>();

    for (int i = 0; i < riderIds.size(); i++) {
      final int idx = i;
      final String riderId = riderIds.get(idx);
      final Point riderPoint = riderPoints.get(idx);

      CompletableFuture<Map.Entry<String, Double>> future = calculateSingleEta(
          riderId, riderPoint, userLat, userLon
      );
      futures.add(future);
    }

    // 모든 결과 대기 및 맵으로 변환
    Map<String, Double> result = futures.stream()
        .map(CompletableFuture::join)
        .filter(entry -> entry != null && entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    log.info("Calculated ETA for {} out of {} riders", result.size(), riderIds.size());

    return result;
  }

  /**
   * 단일 라이더의 ETA 계산 (비동기) - @Async로 Virtual Thread에서 실행
   */
  @Async("deliveryVirtualThreadExecutor")
  public CompletableFuture<Map.Entry<String, Double>> calculateSingleEta(
      String riderId, Point riderPoint, double userLat, double userLon
  ) {
    try {
      WebClient webClient = webClientBuilder.baseUrl(KAKAO_BASE_URL).build();

      Map<String, Object> response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/directions")
              .queryParam("origin", riderPoint.getX() + "," + riderPoint.getY()) // lon,lat
              .queryParam("destination", userLon + "," + userLat)
              .build())
          .header("Authorization", "KakaoAK " + kakaoApiKey)
          .retrieve()
          .bodyToMono(Map.class)
          .block(); // Virtual Thread에서는 block() 안전!

      if (response != null) {
        Map<String, Object> routes = (Map<String, Object>) ((List<?>) response.get("routes"))
            .get(0);
        Map<String, Object> summary = (Map<String, Object>) routes.get("summary");
        Double duration = ((Number) summary.get("duration")).doubleValue(); // 초 단위
        double etaMinutes = duration / 60.0; // 분 단위 변환

        return CompletableFuture.completedFuture(Map.entry(riderId, etaMinutes));
      }
    } catch (Exception e) {
      log.warn("Failed to calculate ETA for rider {}: {}", riderId, e.getMessage());
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * 상점 <-> 주문자 사이 거리 계산 (동기식) - Virtual Thread에서 블로킹 호출해도 효율적
   */
  public Map<String, Double> getDistance(
      double storeLat, double storeLon,
      double userLat, double userLon
  ) {
    WebClient webClient = webClientBuilder.baseUrl(KAKAO_BASE_URL).build();

    // 가상 스레드 풀 사용
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<Map<String, Double>> future = executor.submit(() -> {
        Map<String, Object> response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/directions")
                .queryParam("origin", storeLon + "," + storeLat)
                .queryParam("destination", userLon + "," + userLat)
                .build())
            .header("Authorization", "KakaoAK " + kakaoApiKey)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response == null || !response.containsKey("routes")) {
          throw new IllegalStateException("Invalid API response");
        }

        Map<String, Object> routes = (Map<String, Object>) ((List<?>) response.get("routes")).get(
            0);
        Map<String, Object> summary = (Map<String, Object>) routes.get("summary");
        Double distanceM = ((Number) summary.get("distance")).doubleValue(); // m 단위
        double distanceKm = Math.round((distanceM / 1000.0) * 100.0) / 100.0;

        Map<String, Double> result = new HashMap<>();
        result.put("distance", distanceKm);
        return result;
      });
      return future.get();

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException("Distance calculation failed", e);
    }
  }
}
