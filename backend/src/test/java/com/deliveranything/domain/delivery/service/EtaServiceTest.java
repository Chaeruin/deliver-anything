package com.deliveranything.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("EtaService 단위 테스트")
class EtaServiceTest {

  @InjectMocks
  private EtaService etaService;

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock
  private WebClient.RequestHeadersSpec requestHeadersSpec;
  @Mock
  private WebClient.ResponseSpec responseSpec;

  @BeforeEach
  void setUp() {
    // Inject test api key
    ReflectionTestUtils.setField(etaService, "kakaoApiKey", "test-api-key");

    // Mock WebClient chain
    when(webClientBuilder.baseUrl("https://apis-navi.kakaomobility.com/v1")).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }

  @Test
  @DisplayName("거리 계산 - 정상 응답")
  void getDistance_returnsCorrectDistance() {
    // Given
    Map<String, Object> mockResponse = Map.of(
        "routes", List.of(
            Map.of("summary", Map.of(
                "distance", 5000.0, // meters
                "duration", 1500.0  // seconds
            ))
        )
    );
    when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

    // When
    Map<String, Double> result = etaService.getDistance(37.5, 127.0, 37.6, 127.1);

    // Then
    assertThat(result.get("distance")).isEqualTo(5.0);
  }

  @Test
  @DisplayName("거리 계산 - API 오류 시 예외 발생")
  void getDistance_apiError_throwsException() {
    // Given
    when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("API Error")));

    // When / Then
    try {
      etaService.getDistance(37.5, 127.0, 37.6, 127.1);
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("Distance calculation failed");
    }
  }

  @Test
  @DisplayName("여러 라이더 ETA 계산 - 정상 케이스")
  void getEtaForMultiple_returnsEtas() {
    // Given
    List<Point> riderPoints = List.of(new Point(127.0, 37.5), new Point(127.1, 37.6));
    List<String> riderIds = List.of("rider1", "rider2");

    Map<String, Object> mockResponse1 = Map.of(
        "routes", List.of(Map.of("summary", Map.of("distance", 900.0, "duration", 900.0)))
    );
    Map<String, Object> mockResponse2 = Map.of(
        "routes", List.of(Map.of("summary", Map.of("distance", 1200.0, "duration", 1200.0)))
    );

    when(responseSpec.bodyToMono(Map.class))
        .thenReturn(Mono.just(mockResponse1))
        .thenReturn(Mono.just(mockResponse2));

    // When
    Map<String, Double> result = etaService.getEtaForMultiple(37.6, 127.1, riderPoints, riderIds);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get("rider1")).isEqualTo(15.0);
    assertThat(result.get("rider2")).isEqualTo(20.0);
  }

  @Test
  @DisplayName("여러 라이더 ETA 계산 - 일부 실패 시 성공만 반환")
  void getEtaForMultiple_partialFailure_returnsSuccessful() {
    // Given
    List<Point> riderPoints = List.of(new Point(127.0, 37.5), new Point(127.1, 37.6));
    List<String> riderIds = List.of("rider1", "rider2");

    Map<String, Object> mockResponse = Map.of(
        "routes", List.of(Map.of("summary", Map.of("distance", 900.0, "duration", 900.0)))
    );

    when(responseSpec.bodyToMono(Map.class))
        .thenReturn(Mono.just(mockResponse))
        .thenReturn(Mono.error(new RuntimeException("API Error")));

    // When
    Map<String, Double> result = etaService.getEtaForMultiple(37.6, 127.1, riderPoints, riderIds);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get("rider1")).isEqualTo(15.0);
  }
}
