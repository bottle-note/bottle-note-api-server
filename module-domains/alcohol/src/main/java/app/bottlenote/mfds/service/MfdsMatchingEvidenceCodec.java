package app.bottlenote.mfds.service;

import app.bottlenote.mfds.dto.request.MfdsMatchingReferenceSnapshotItem;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 기존 JSON·TEXT 컬럼에 저장할 실행 메타데이터와 점수 근거의 직렬화를 담당한다. */
@Component
@RequiredArgsConstructor
public class MfdsMatchingEvidenceCodec {
  private final ObjectMapper objectMapper;

  public String encodeDetail(MfdsMatchScoreDetailItem detail) {
    return write(detail);
  }

  public MfdsMatchScoreDetailItem decodeDetail(String json) {
    try {
      return objectMapper.readValue(json, MfdsMatchScoreDetailItem.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("저장된 MFDS 점수 근거를 읽을 수 없습니다", e);
    }
  }

  public String encodeStats(int alcohols, int distilleries, int regions) {
    return write(
        Map.of("alcoholCount", alcohols, "distilleryCount", distilleries, "regionCount", regions));
  }

  public String referenceHash(MfdsMatchingReferenceSnapshotItem references) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(write(references).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
    }
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("MFDS 매칭 실행 직렬화 실패", e);
    }
  }
}
