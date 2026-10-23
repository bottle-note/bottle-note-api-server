package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.Operator;
import app.bottlenote.alcohols.exception.ScenarioException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

public record ScenarioPredicate(Operator operator, JsonNode value) {
  public boolean matches(
      app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind kind,
      String choiceId,
      java.math.BigDecimal min,
      java.math.BigDecimal max) {
    if (operator == Operator.ALWAYS) return true;
    if (kind == app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind.ANY) return false;
    return switch (operator) {
      case EQ -> value.textValue().equals(choiceId);
      case GTE -> min.compareTo(value.decimalValue()) >= 0;
      case LTE -> max.compareTo(value.decimalValue()) <= 0;
      case ALWAYS -> true;
    };
  }

  @JsonAnySetter
  public void rejectUnknown(String key, JsonNode value) {
    throw ScenarioException.invalid("predicate에 미지원 필드가 있습니다: " + key);
  }
}
