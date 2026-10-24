package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import app.bottlenote.global.pagination.KeysetPageResponse;
import java.math.BigDecimal;
import java.util.List;

public record ScenarioEvaluationResponse(
    Long scenarioId,
    String title,
    Long versionId,
    State state,
    Long nextQuestionId,
    List<Long> path,
    List<ScenarioAnswerView> answers,
    ScenarioQuestion question,
    long candidateCount,
    KeysetPageResponse<List<ScenarioCandidate>> candidates) {
  public enum State {
    QUESTION,
    USER_FINISHED,
    SCENARIO_END
  }

  public record ScenarioAnswerView(
      Long questionId, String optionId, String label, boolean applied) {}

  public record ScenarioQuestion(Long questionId, String title, List<ScenarioOption> options) {}

  public record ScenarioOption(
      String optionId,
      String label,
      AnswerKind kind,
      BigDecimal min,
      BigDecimal max,
      boolean minExclusive,
      long candidateCount,
      boolean selectable) {}

  public record ScenarioCandidate(
      Long alcoholId,
      String korName,
      String engName,
      String imageUrl,
      String korCategory,
      String abv) {}
}
