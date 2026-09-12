package app.bottlenote.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CursorKeys 단위 테스트")
class CursorKeysTest {

  @Test
  @DisplayName("optionalDate는 값이 없으면 null을 반환한다")
  void optionalDate는_값이_없으면_null이다() {
    CursorClaims claims = claims(Map.of("id", "1"));

    assertThat(CursorKeys.optionalDate(claims, "processedDate")).isNull();
    assertThat(CursorKeys.optionalDate(null, "processedDate")).isNull();
  }

  @Test
  @DisplayName("optionalDate는 날짜 문자열을 LocalDate로 파싱한다")
  void optionalDate는_날짜를_파싱한다() {
    CursorClaims claims = claims(Map.of("processedDate", "2026-08-01"));

    assertThat(CursorKeys.optionalDate(claims, "processedDate"))
        .isEqualTo(LocalDate.of(2026, 8, 1));
  }

  @Test
  @DisplayName("optionalDate는 파싱에 실패하면 INVALID_CURSOR를 던진다")
  void optionalDate는_파싱_실패_시_INVALID_CURSOR이다() {
    CursorClaims claims = claims(Map.of("processedDate", "not-a-date"));

    assertThatThrownBy(() -> CursorKeys.optionalDate(claims, "processedDate"))
        .isInstanceOf(PaginationException.class)
        .extracting(exception -> ((PaginationException) exception).getExceptionCode())
        .isEqualTo(PaginationExceptionCode.INVALID_CURSOR);
  }

  private static CursorClaims claims(Map<String, String> sortKeys) {
    Instant now = Instant.parse("2026-08-15T00:00:00Z");
    return new CursorClaims(1, "v1", "hash", sortKeys, now, now, Map.of());
  }
}
