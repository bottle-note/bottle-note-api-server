package app.bottlenote.global.pagination;

import java.time.LocalDateTime;
import java.util.Map;

public final class TimeIdCursor {

  private TimeIdCursor() {}

  public static Map<String, String> keys(LocalDateTime time, Long id) {
    return Map.of("t", time.toString(), "id", String.valueOf(id));
  }

  public static LocalDateTime time(CursorClaims claims) {
    try {
      return LocalDateTime.parse(require(claims, "t"));
    } catch (RuntimeException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  public static Long id(CursorClaims claims) {
    try {
      return Long.valueOf(require(claims, "id"));
    } catch (RuntimeException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  private static String require(CursorClaims claims, String key) {
    String value = claims.sortKeys().get(key);
    if (value == null || value.isBlank()) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
    return value;
  }
}
