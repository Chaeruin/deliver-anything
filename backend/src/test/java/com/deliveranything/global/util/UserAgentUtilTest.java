package com.deliveranything.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAgentUtilTest {

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private UserAgentUtil userAgentUtil;

  @Test
  @DisplayName("PC User-Agent 파싱 테스트")
  void extractDeviceInfoPcTest() {
    String userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    when(request.getHeader("User-Agent")).thenReturn(userAgentString);

    String deviceInfo = userAgentUtil.extractDeviceInfo(request);
    assertThat(deviceInfo).containsIgnoringCase("PC").containsIgnoringCase("Windows 10").containsIgnoringCase("Chrome");
  }

  @Test
  @DisplayName("Mobile User-Agent 파싱 테스트")
  void extractDeviceInfoMobileTest() {
    String userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1";
    when(request.getHeader("User-Agent")).thenReturn(userAgentString);

    String deviceInfo = userAgentUtil.extractDeviceInfo(request);
    assertThat(deviceInfo).containsIgnoringCase("Mobile").containsIgnoringCase("Mac OS X").containsIgnoringCase("Safari");
  }

  @Test
  @DisplayName("Tablet User-Agent 파싱 테스트")
  void extractDeviceInfoTabletTest() {
    String userAgentString = "Mozilla/5.0 (iPad; CPU OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/83.0.4103.88 Mobile/15E148 Safari/604.1";
    when(request.getHeader("User-Agent")).thenReturn(userAgentString);

    String deviceInfo = userAgentUtil.extractDeviceInfo(request);
    assertThat(deviceInfo).containsIgnoringCase("Tablet").containsIgnoringCase("Mac OS X").containsIgnoringCase("Chrome");
  }

  @Test
  @DisplayName("User-Agent 헤더가 null일 경우 처리 테스트")
  void extractDeviceInfoNullUserAgentTest() {
    when(request.getHeader("User-Agent")).thenReturn(null);

    String deviceInfo = userAgentUtil.extractDeviceInfo(request);
    assertThat(deviceInfo).isEqualTo("Unknown Device");
  }

  @Test
  @DisplayName("User-Agent 헤더가 비어있을 경우 처리 테스트")
  void extractDeviceInfoEmptyUserAgentTest() {
    when(request.getHeader("User-Agent")).thenReturn("");

    String deviceInfo = userAgentUtil.extractDeviceInfo(request);
    assertThat(deviceInfo).isEqualTo("Unknown Device");
  }

  @Test
  @DisplayName("간단한 기기 정보 추출 테스트 (PC)")
  void extractSimpleDeviceInfoPcTest() {
    String userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    when(request.getHeader("User-Agent")).thenReturn(userAgentString);

    String simpleDeviceInfo = userAgentUtil.extractSimpleDeviceInfo(request);
    assertThat(simpleDeviceInfo).containsIgnoringCase("PC").containsIgnoringCase("Windows 10");
  }

  @Test
  @DisplayName("간단한 기기 정보 추출 테스트 (Mobile)")
  void extractSimpleDeviceInfoMobileTest() {
    String userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1";
    when(request.getHeader("User-Agent")).thenReturn(userAgentString);

    String simpleDeviceInfo = userAgentUtil.extractSimpleDeviceInfo(request);
    assertThat(simpleDeviceInfo).containsIgnoringCase("Mobile").containsIgnoringCase("Mac OS X");
  }

  @Test
  @DisplayName("간단한 기기 정보 추출 시 User-Agent 헤더가 null일 경우 처리 테스트")
  void extractSimpleDeviceInfoNullUserAgentTest() {
    when(request.getHeader("User-Agent")).thenReturn(null);

    String simpleDeviceInfo = userAgentUtil.extractSimpleDeviceInfo(request);
    assertThat(simpleDeviceInfo).isEqualTo("Unknown");
  }
}