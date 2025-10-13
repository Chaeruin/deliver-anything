package com.deliveranything.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorUtilTest {

  @Test
  @DisplayName("단일 키 인코딩 및 디코딩 테스트")
  void encodeAndDecodeSingleKeyTest() {
    String key = "testKey";
    String encoded = CursorUtil.encode(key);
    assertThat(encoded).isNotNull();

    Object[] decoded = CursorUtil.decode(encoded);
    assertThat(decoded).isNotNull().hasSize(1);
    assertThat(decoded[0]).isEqualTo(key);
  }

  @Test
  @DisplayName("여러 키 인코딩 및 디코딩 테스트")
  void encodeAndDecodeMultipleKeysTest() {
    String key1 = "key1";
    Long key2 = 123L;
    String encoded = CursorUtil.encode(key1, key2);
    assertThat(encoded).isNotNull();

    Object[] decoded = CursorUtil.decode(encoded);
    assertThat(decoded).isNotNull().hasSize(2);
    assertThat(decoded[0]).isEqualTo(key1);
    assertThat(decoded[1].toString()).isEqualTo(key2.toString());
  }
  @Test
  @DisplayName("null 키 인코딩 시 null 반환 테스트")
  void encodeNullKeyTest() {
    String encoded = CursorUtil.encode((Object[]) null);
    assertThat(encoded).isNull();
  }

  @Test
  @DisplayName("빈 키 배열 인코딩 시 null 반환 테스트")
  void encodeEmptyKeysTest() {
    String encoded = CursorUtil.encode();
    assertThat(encoded).isNull();
  }

  @Test
  @DisplayName("null 커서 디코딩 시 null 반환 테스트")
  void decodeNullCursorTest() {
    Object[] decoded = CursorUtil.decode(null);
    assertThat(decoded).isNull();
  }

  @Test
  @DisplayName("빈 문자열 커서 디코딩 시 null 반환 테스트")
  void decodeEmptyCursorTest() {
    Object[] decoded = CursorUtil.decode("");
    assertThat(decoded).isNull();
  }

  @Test
  @DisplayName("유효하지 않은 커서 디코딩 시 null 반환 테스트")
  void decodeInvalidCursorTest() {
    Object[] decoded = CursorUtil.decode("invalid-base64-string");
    assertThat(decoded).isNull();
  }

  @Test
  @DisplayName("숫자 키 인코딩 및 디코딩 테스트")
  void encodeAndDecodeNumberKeyTest() {
    Long key = 456789L;
    String encoded = CursorUtil.encode(key);
    assertThat(encoded).isNotNull();

    Object[] decoded = CursorUtil.decode(encoded);
    assertThat(decoded).isNotNull().hasSize(1);
    assertThat(decoded[0].toString()).isEqualTo(key.toString());
  }

  @Test
  @DisplayName("부동 소수점 키 인코딩 및 디코딩 테스트")
  void encodeAndDecodeDoubleKeyTest() {
    Double key = 123.45;
    String encoded = CursorUtil.encode(key);
    assertThat(encoded).isNotNull();

    Object[] decoded = CursorUtil.decode(encoded);
    assertThat(decoded).isNotNull().hasSize(1);
    // ObjectMapper는 Double을 BigDecimal로 디코딩할 수 있으므로, String으로 비교
    assertThat(decoded[0].toString()).isEqualTo(key.toString());
  }
}