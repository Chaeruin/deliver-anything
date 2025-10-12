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
      return Base64.getEncoder().encodeToString(jsonBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to encode cursor", e);
    }
  }

  public static Object[] decode(String cursor) {
    if (!StringUtils.hasText(cursor)) return null;
    try {
      byte[] decodedBytes = Base64.getDecoder().decode(cursor);
      return OBJECT_MAPPER.readValue(decodedBytes, Object[].class);
    } catch (Exception e) {
      return null;
    }
  }
}