package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import app.bottlenote.alcohols.domain.ScenarioAnswer;
import java.math.BigDecimal;

public record ScenarioAnswerItem(
    Long questionId, AnswerKind kind, String choiceId, BigDecimal min, BigDecimal max) {
  public static ScenarioAnswerItem from(ScenarioAnswer a) {
    return new ScenarioAnswerItem(a.questionId(), a.kind(), a.choiceId(), a.min(), a.max());
  }
}
