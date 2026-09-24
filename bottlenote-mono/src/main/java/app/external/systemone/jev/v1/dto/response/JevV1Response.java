package app.external.systemone.jev.v1.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Jev v1 POST /v1/systemone 응답 본문. 알 수 없는 필드는 무시해 하위 호환 추가에 대비한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JevV1Response(String model, Map<String, Answer> answers, Usage usage) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Answer(
      String type,
      String choice,
      Double score,
      Double noul,
      Map<String, Double> probabilities,
      Map<String, String> legend,
      Double confidence) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Usage(
      @JsonProperty("input_tokens") Long inputTokens,
      @JsonProperty("output_tokens") Long outputTokens) {}
}
