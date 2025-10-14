package com.deliveranything.global.infra;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsService 테스트")
public class SmsServiceImplTest {

  @Mock
  private SmsService smsService;

  @Test
  @DisplayName("sms 보내기 성공")
  void sendSms_green() {
    assertThatCode(() -> smsService.sendSms("010-1234-5678", "sms 보내기 테스트"))
        .doesNotThrowAnyException();
  }
}
