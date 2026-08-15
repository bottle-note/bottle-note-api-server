package app.bottlenote.global.pagination;

import java.time.LocalDateTime;

/** 커서 sortKeys/extra 값 접근 헬퍼. 키 누락과 파싱 실패를 모두 INVALID_CURSOR(400)로 변환한다. */
public final class CursorKeys {

  private CursorKeys() {}

  /** 값이 없거나 공백이면 INVALID_CURSOR. */
  public static String require(CursorClaims claims, String key) {
    String value = claims == null ? null : claims.sortKeys().get(key);
    if (value == null || value.isBlank()) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
    return value;
  }

  /** 값이 없으면 null. 정렬 값이 NULL인 행을 커서에 담을 때 쓴다. */
  public static String optional(CursorClaims claims, String key) {
    String value = claims == null ? null : claims.sortKeys().get(key);
    return value == null || value.isBlank() ? null : value;
  }

  public static Long requireLong(CursorClaims claims, String key) {
    return parseLong(require(claims, key));
  }

  public static Integer requireInt(CursorClaims claims, String key) {
    String value = require(claims, key);
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  public static Double requireDouble(CursorClaims claims, String key) {
    String value = require(claims, key);
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  public static Boolean requireBoolean(CursorClaims claims, String key) {
    return Boolean.valueOf(require(claims, key));
  }

  public static LocalDateTime requireTime(CursorClaims claims, String key) {
    String value = require(claims, key);
    try {
      return LocalDateTime.parse(value);
    } catch (RuntimeException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  /** 값이 없으면 null, 있으면 파싱 실패 시 INVALID_CURSOR. */
  public static Long optionalLong(CursorClaims claims, String key) {
    String value = optional(claims, key);
    return value == null ? null : parseLong(value);
  }

  public static Double optionalDouble(CursorClaims claims, String key) {
    String value = optional(claims, key);
    if (value == null) {
      return null;
    }
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  public static LocalDateTime optionalTime(CursorClaims claims, String key) {
    String value = optional(claims, key);
    if (value == null) {
      return null;
    }
    try {
      return LocalDateTime.parse(value);
    } catch (RuntimeException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }

  /** extra 값이 없거나 숫자가 아니면 INVALID_CURSOR. RANDOM seed 복원에 쓴다. */
  public static Long requireExtraLong(CursorClaims claims, String key) {
    String value = claims == null ? null : claims.extra().get(key);
    if (value == null || value.isBlank()) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
    return parseLong(value);
  }

  private static Long parseLong(String value) {
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException exception) {
      throw new PaginationException(PaginationExceptionCode.INVALID_CURSOR);
    }
  }
}
