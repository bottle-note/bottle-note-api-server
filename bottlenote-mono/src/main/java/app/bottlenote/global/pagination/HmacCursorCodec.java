package app.bottlenote.global.pagination;

import static app.bottlenote.global.pagination.PaginationExceptionCode.CURSOR_CONTEXT_MISMATCH;
import static app.bottlenote.global.pagination.PaginationExceptionCode.CURSOR_EXPIRED;
import static app.bottlenote.global.pagination.PaginationExceptionCode.INVALID_CURSOR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacCursorCodec {

  public static final Duration TTL = Duration.ofHours(1);
  public static final Duration IAT_SKEW = Duration.ofSeconds(30);
  public static final int MAX_TOKEN_BYTES = 4096;
  public static final int CURRENT_VERSION = 1;

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final CursorProperties properties;
  private final Clock clock;

  public HmacCursorCodec(CursorProperties properties, Clock clock) {
    Objects.requireNonNull(properties, "properties");
    properties.validate();
    this.properties = properties;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public String encode(String context, Map<String, String> sortKeys) {
    return encode(context, sortKeys, Map.of());
  }

  public String encode(String context, Map<String, String> sortKeys, Map<String, String> extra) {
    Instant now = Instant.now(clock);
    String keyId = properties.getCurrentKeyId();
    Header header = new Header("HS256", "BNCUR", keyId);
    Payload payload =
        new Payload(
            CURRENT_VERSION,
            keyId,
            hashContext(context),
            copyOf(sortKeys),
            now.getEpochSecond(),
            now.plus(TTL).getEpochSecond(),
            copyOf(extra));
    String headerPart = base64Url(writeJson(header));
    String payloadPart = base64Url(writeJson(payload));
    String signingInput = headerPart + "." + payloadPart;
    String token = signingInput + "." + sign(signingInput, properties.currentSecretBytes());
    if (utf8Length(token) > MAX_TOKEN_BYTES) {
      throw new PaginationException(INVALID_CURSOR);
    }
    return token;
  }

  public CursorClaims decode(String token) {
    Payload payload = verifiedPayload(token);
    Instant now = Instant.now(clock);
    Instant issuedAt = Instant.ofEpochSecond(payload.iat());
    Instant expiresAt = Instant.ofEpochSecond(payload.exp());
    if (issuedAt.isAfter(now.plus(IAT_SKEW))) {
      throw new PaginationException(INVALID_CURSOR);
    }
    if (!now.isBefore(expiresAt)) {
      throw new PaginationException(CURSOR_EXPIRED);
    }
    return new CursorClaims(
        payload.v(),
        payload.kid(),
        payload.ctx(),
        copyOf(payload.keys()),
        issuedAt,
        expiresAt,
        copyOf(payload.extra()));
  }

  public CursorClaims verify(String token, String context) {
    CursorClaims claims = decode(token);
    if (!constantTimeEquals(claims.contextHash(), hashContext(context))) {
      throw new PaginationException(CURSOR_CONTEXT_MISMATCH);
    }
    return claims;
  }

  public static String hashContext(String context) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((context == null ? "" : context).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required", exception);
    }
  }

  private Payload verifiedPayload(String token) {
    if (token == null || token.isBlank() || utf8Length(token) > MAX_TOKEN_BYTES) {
      throw new PaginationException(INVALID_CURSOR);
    }
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) {
      throw new PaginationException(INVALID_CURSOR);
    }
    Header header = readJson(decodeBase64Url(parts[0]), Header.class);
    if (header == null || !"HS256".equals(header.alg()) || isBlank(header.kid())) {
      throw new PaginationException(INVALID_CURSOR);
    }
    byte[] secret =
        properties
            .secretFor(header.kid())
            .orElseThrow(() -> new PaginationException(INVALID_CURSOR));
    String signingInput = parts[0] + "." + parts[1];
    byte[] expected = hmac(signingInput, secret);
    byte[] actual = decodeBase64Url(parts[2]);
    if (!MessageDigest.isEqual(expected, actual)) {
      throw new PaginationException(INVALID_CURSOR);
    }
    Payload payload = readJson(decodeBase64Url(parts[1]), Payload.class);
    if (payload == null
        || payload.v() != CURRENT_VERSION
        || isBlank(payload.kid())
        || isBlank(payload.ctx())
        || !header.kid().equals(payload.kid())) {
      throw new PaginationException(INVALID_CURSOR);
    }
    return payload;
  }

  private static String sign(String signingInput, byte[] secret) {
    return base64Url(hmac(signingInput, secret));
  }

  private static byte[] hmac(String signingInput, byte[] secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret, HMAC_SHA256));
      return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("HMAC-SHA256 is required", exception);
    }
  }

  private static byte[] writeJson(Object value) {
    try {
      return MAPPER.writeValueAsBytes(value);
    } catch (Exception exception) {
      throw new PaginationException(INVALID_CURSOR);
    }
  }

  private static <T> T readJson(byte[] json, Class<T> type) {
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception exception) {
      throw new PaginationException(INVALID_CURSOR);
    }
  }

  private static byte[] decodeBase64Url(String value) {
    try {
      return Base64.getUrlDecoder().decode(value);
    } catch (IllegalArgumentException exception) {
      throw new PaginationException(INVALID_CURSOR);
    }
  }

  private static String base64Url(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private static Map<String, String> copyOf(Map<String, String> values) {
    return values == null || values.isEmpty() ? Map.of() : Map.copyOf(values);
  }

  private static boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(leftBytes, rightBytes);
  }

  private static int utf8Length(String value) {
    return value.getBytes(StandardCharsets.UTF_8).length;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record Header(String alg, String typ, String kid) {}

  private record Payload(
      int v,
      String kid,
      String ctx,
      Map<String, String> keys,
      long iat,
      long exp,
      Map<String, String> extra) {}
}
