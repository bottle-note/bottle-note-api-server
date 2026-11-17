package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioException.invalid;

import app.bottlenote.alcohols.domain.ScenarioAnswer.Selection;
import app.bottlenote.global.pagination.CursorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioOptionCodec {
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private final CursorProperties properties;
  private final ObjectMapper mapper;

  public String context(Long scenarioId, Long versionId, List<Selection> prefix) {
    // 표기 문구와 서명 키 교체가 선행 답변의 의미를 바꾸지 않는다.
    var values =
        prefix.stream()
            .map(
                s ->
                    List.of(
                        s.answer().questionId(),
                        s.answer().kind().name(),
                        s.answer().choiceId() == null ? "" : s.answer().choiceId(),
                        decimal(s.answer().min()),
                        decimal(s.answer().max()),
                        s.minExclusive()))
            .toList();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(json(List.of("scenario-options-v1", scenarioId, versionId, values))));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String encode(String context, Selection selection) {
    String keyId =
        ENCODER.encodeToString(properties.getCurrentKeyId().getBytes(StandardCharsets.UTF_8));
    String body = ENCODER.encodeToString(json(new Payload(1, context, selection)));
    String input = keyId + "." + body;
    return input + "." + ENCODER.encodeToString(sign(input, properties.currentSecretBytes()));
  }

  public Selection decode(String token, String context, Long questionId) {
    if (token == null || token.length() > 2048) throw invalid("선택지를 다시 조회해 주세요.");
    try {
      String[] parts = token.split("\\.", -1);
      if (parts.length != 3) throw invalid("선택지 형식이 올바르지 않습니다.");
      String keyId = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      byte[] secret =
          properties.secretFor(keyId).orElseThrow(() -> invalid("선택지 서명 키가 만료됐습니다. 다시 선택해 주세요."));
      byte[] expected = sign(parts[0] + "." + parts[1], secret);
      if (!MessageDigest.isEqual(expected, Base64.getUrlDecoder().decode(parts[2])))
        throw invalid("변조된 선택지입니다.");
      Payload p = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Payload.class);
      if (p.format() != 1
          || !context.equals(p.context())
          || p.selection() == null
          || p.selection().answer() == null
          || !questionId.equals(p.selection().answer().questionId()))
        throw invalid("다른 시나리오·질문 또는 선행 답변의 선택지입니다.");
      return p.selection();
    } catch (IllegalArgumentException | java.io.IOException e) {
      throw invalid("선택지 형식이 올바르지 않습니다.");
    }
  }

  private String decimal(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private byte[] json(Object value) {
    try {
      return mapper.writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] sign(String input, byte[] secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return mac.doFinal(
          ("bottlenote-scenario-option-v1:" + input).getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private record Payload(int format, String context, Selection selection) {}
}
