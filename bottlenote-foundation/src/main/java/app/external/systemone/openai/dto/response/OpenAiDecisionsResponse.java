package app.external.systemone.openai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** OpenAI POST /v1/decisions 응답 본문. 알 수 없는 필드는 무시해 하위 호환 추가에 대비한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiDecisionsResponse(String id, String model, List<Answer> answers, Usage usage) {

  /**
   * @param type predicate | choice | score | refusal
   * @param probability predicate 전용. 참일 확률
   * @param probabilities choice는 value가 선택지 키, score는 value가 0부터 시작하는 단계 인덱스
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Answer(
      String type,
      String name,
      Double probability,
      String choice,
      Double score,
      Double confidence,
      List<Probability> probabilities) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Probability(Object value, String label, Double probability) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Usage(
      @JsonProperty("input_tokens") Long inputTokens,
      @JsonProperty("output_tokens") Long outputTokens) {}
}
