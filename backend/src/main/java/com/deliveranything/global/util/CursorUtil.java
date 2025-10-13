package com.deliveranything.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.springframework.util.StringUtils;

public final class CursorUtil {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private CursorUtil() {}

  public static String encode(Object... keys) {
    if (keys == null || keys.length == 0) return null;
    try {
      byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(keys);
      return Base64.getUrlEncoder().encodeToString(jsonBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to encode cursor", e);
    }
  }

  public static Object[] decode(String cursor) {
    if (!StringUtils.hasText(cursor)) return null;
    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
      return OBJECT_MAPPER.readValue(decodedBytes, Object[].class);
    } catch (Exception e) {
      return null;
    }
  }

// 개선 버전 커서 조건을 클래스로 만들어서 관리
//  // 커서 클래스 인코드
//  public static String encode(Object cursorObj) {
//    if (cursorObj == null) return null;
//    try {
//      byte[] jsonBytes = OBJECT_MAPPER.writeValueAsBytes(cursorObj);
//      return Base64.getUrlEncoder().encodeToString(jsonBytes); // URL-safe
//    } catch (Exception e) {
//      throw new RuntimeException("Failed to encode cursor", e);
//    }
//  }
//
//  // 커서 클래스 디코드
//  public static <T> T decode(String cursor, Class<T> clazz) {
//    if (!StringUtils.hasText(cursor)) return null;
//    try {
//      byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
//      return OBJECT_MAPPER.readValue(decodedBytes, clazz);
//    } catch (Exception e) {
//      return null;
//    }
//  }
}