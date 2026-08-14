package app.bottlenote.global.pagination;

import java.time.Instant;
import java.util.Map;

public record CursorClaims(
    int version,
    String keyId,
    String contextHash,
    Map<String, String> sortKeys,
    Instant issuedAt,
    Instant expiresAt,
    Map<String, String> extra) {

  public CursorClaims {
    sortKeys = sortKeys == null ? Map.of() : Map.copyOf(sortKeys);
    extra = extra == null ? Map.of() : Map.copyOf(extra);
  }
}
