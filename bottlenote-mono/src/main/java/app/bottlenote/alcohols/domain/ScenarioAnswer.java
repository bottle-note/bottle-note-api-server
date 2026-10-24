package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import app.bottlenote.alcohols.exception.ScenarioException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ScenarioAnswer(
    @NotNull @Positive Long questionId,
    @NotNull AnswerKind kind,
    String choiceId,
    @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal min,
    @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal max) {
  @JsonAnySetter
  public void rejectUnknown(String key, JsonNode value) {
    throw ScenarioException.invalid("답변에 미지원 필드가 있습니다: " + key);
  }

  public record Selection(ScenarioAnswer answer, boolean minExclusive, String label) {}

  public record ScenarioContribution(
      Long questionId,
      AlcoholQuestionSource source,
      Long attributeId,
      String operator,
      String value) {}
}
