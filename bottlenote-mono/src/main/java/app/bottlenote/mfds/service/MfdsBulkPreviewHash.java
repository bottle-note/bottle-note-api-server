package app.bottlenote.mfds.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * 미리보기 발급 기록 안의 내부 상태 해시. 클라이언트에 주는 난수 previewToken이 아니다. 확정은 잠근 현재 행으로 이 해시를 다시 계산해 Redis에 저장된 값과
 * 비교한다.
 */
final class MfdsBulkPreviewHash {

  private static final String VERSION = "mfds-bulk-preview-v2";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MfdsBulkPreviewHash() {}

  /** 순서가 고정된 상태 값 목록을 JSON으로 직렬화해 SHA-256으로 요약한다. */
  static String of(List<?> state) {
    try {
      byte[] json = MAPPER.writeValueAsBytes(List.of(VERSION, state));
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("일괄 매칭 미리보기 검증값을 계산하지 못했습니다.", exception);
    }
  }

  static boolean matches(String expected, String actual) {
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        Objects.toString(actual, "").getBytes(StandardCharsets.UTF_8));
  }
}
