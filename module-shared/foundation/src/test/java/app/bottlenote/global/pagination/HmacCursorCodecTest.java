package app.bottlenote.global.pagination;

import static app.bottlenote.global.pagination.PaginationExceptionCode.CURSOR_CONTEXT_MISMATCH;
import static app.bottlenote.global.pagination.PaginationExceptionCode.CURSOR_EXPIRED;
import static app.bottlenote.global.pagination.PaginationExceptionCode.INVALID_CURSOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("HMAC 커서 코덱 단위 테스트")
class HmacCursorCodecTest {

  private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  @DisplayName("서명 후 같은 컨텍스트로 검증하면 정렬 키와 extra를 복원한다")
  void encode_then_verify_restores_claims() {
    HmacCursorCodec codec = codec(currentOnly());

    String token =
        codec.encode(
            "reviews:POPULAR:DESC",
            Map.of("id", "42", "rating", "4.5"),
            Map.of("seed", "abc", "effectiveDate", "2026-08-15"));

    CursorClaims claims = codec.verify(token, "reviews:POPULAR:DESC");

    assertThat(claims.version()).isEqualTo(1);
    assertThat(claims.keyId()).isEqualTo("v1");
    assertThat(claims.sortKeys()).containsEntry("id", "42").containsEntry("rating", "4.5");
    assertThat(claims.extra())
        .containsEntry("seed", "abc")
        .containsEntry("effectiveDate", "2026-08-15");
    assertThat(claims.issuedAt()).isEqualTo(NOW);
    assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    assertThat(claims.contextHash()).isEqualTo(HmacCursorCodec.hashContext("reviews:POPULAR:DESC"));
  }

  @Test
  @DisplayName("서명이 변조되면 INVALID_CURSOR를 던진다")
  void tampered_signature_throws_invalid_cursor() {
    HmacCursorCodec codec = codec(currentOnly());
    String token = codec.encode("ctx", Map.of("id", "1"));
    String tampered = token.substring(0, token.length() - 2) + "aa";

    assertInvalidCursor(() -> codec.verify(tampered, "ctx"));
  }

  @Test
  @DisplayName("TTL 1시간이 지나면 CURSOR_EXPIRED를 던진다")
  void expired_token_throws_cursor_expired() {
    HmacCursorCodec encoder = codec(currentOnly(), FIXED_CLOCK);
    String token = encoder.encode("ctx", Map.of("id", "1"));
    HmacCursorCodec later =
        codec(
            currentOnly(),
            Clock.fixed(NOW.plus(Duration.ofHours(1)).plusSeconds(1), ZoneOffset.UTC));

    assertThatThrownBy(() -> later.verify(token, "ctx"))
        .isInstanceOf(PaginationException.class)
        .extracting(exception -> ((PaginationException) exception).getExceptionCode())
        .isEqualTo(CURSOR_EXPIRED);
  }

  @Test
  @DisplayName("다른 조회 컨텍스트로 검증하면 CURSOR_CONTEXT_MISMATCH를 던진다")
  void context_mismatch_throws() {
    HmacCursorCodec codec = codec(currentOnly());
    String token = codec.encode("reviews:POPULAR", Map.of("id", "1"));

    assertThatThrownBy(() -> codec.verify(token, "reviews:LATEST"))
        .isInstanceOf(PaginationException.class)
        .extracting(exception -> ((PaginationException) exception).getExceptionCode())
        .isEqualTo(CURSOR_CONTEXT_MISMATCH);
  }

  @Test
  @DisplayName("이전 키로 서명한 커서는 현재 설정에서 검증된다")
  void previous_key_still_verifies() {
    CursorProperties rotated = new CursorProperties();
    rotated.setCurrentKeyId("v2");
    rotated.setCurrentSecret("current-pagination-cursor-secret");
    rotated.setPreviousKeyId("v1");
    rotated.setPreviousSecret("previous-pagination-cursor-secret");

    CursorProperties previous = currentOnly();
    previous.setCurrentSecret("previous-pagination-cursor-secret");

    String token = codec(previous).encode("ctx", Map.of("id", "99"), Map.of("seed", "s1"));
    CursorClaims claims = codec(rotated).verify(token, "ctx");

    assertThat(claims.keyId()).isEqualTo("v1");
    assertThat(claims.sortKeys()).containsEntry("id", "99");
    assertThat(claims.extra()).containsEntry("seed", "s1");
  }

  @Test
  @DisplayName("current-secret이 없으면 설정 검증이 실패한다")
  void missing_secret_fails_validation() {
    CursorProperties properties = new CursorProperties();

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("current-secret");
  }

  @Test
  @DisplayName("iat가 30초를 넘어 미래이면 INVALID_CURSOR를 던진다")
  void future_iat_beyond_skew_is_invalid() {
    HmacCursorCodec encoder = codec(currentOnly(), FIXED_CLOCK);
    String token = encoder.encode("ctx", Map.of("id", "1"));
    HmacCursorCodec earlier =
        codec(currentOnly(), Clock.fixed(NOW.minusSeconds(31), ZoneOffset.UTC));

    assertInvalidCursor(() -> earlier.decode(token));
  }

  @Test
  @DisplayName("빈 토큰과 알 수 없는 kid는 INVALID_CURSOR다")
  void blank_token_and_unknown_kid_are_invalid() {
    HmacCursorCodec codec = codec(currentOnly());
    String token = codec.encode("ctx", Map.of("id", "1"));
    HmacCursorCodec other = codec(key("v9", "other-pagination-cursor-secret"));

    assertInvalidCursor(() -> codec.decode(" "));
    assertInvalidCursor(() -> codec.decode(null));
    assertInvalidCursor(() -> other.decode(token));
  }

  @Test
  @DisplayName("토큰이 4KiB를 넘으면 INVALID_CURSOR를 던진다")
  void oversized_token_is_invalid() {
    HmacCursorCodec codec = codec(currentOnly());

    assertInvalidCursor(() -> codec.decode("a".repeat(HmacCursorCodec.MAX_TOKEN_BYTES + 1)));
  }

  private static void assertInvalidCursor(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOf(PaginationException.class)
        .extracting(exception -> ((PaginationException) exception).getExceptionCode())
        .isEqualTo(INVALID_CURSOR);
  }

  private static HmacCursorCodec codec(CursorProperties properties) {
    return codec(properties, FIXED_CLOCK);
  }

  private static HmacCursorCodec codec(CursorProperties properties, Clock clock) {
    return new HmacCursorCodec(properties, clock);
  }

  private static CursorProperties currentOnly() {
    return key("v1", "test-pagination-cursor-secret");
  }

  private static CursorProperties key(String keyId, String secret) {
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId(keyId);
    properties.setCurrentSecret(secret);
    return properties;
  }
}
