package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.ScenarioInputConfig;
import app.bottlenote.alcohols.domain.ScenarioPredicate;
import java.util.List;

public record ScenarioDefinitionRequest(
    Integer definitionSchemaVersion,
    Long startQuestionId,
    List<ScenarioStageItem> stages,
    List<ScenarioQuestionItem> questions,
    List<ScenarioTransitionItem> transitions) {
  public static ScenarioDefinitionRequest empty() {
    return new ScenarioDefinitionRequest(1, null, List.of(), List.of(), List.of());
  }

  public record ScenarioStageItem(Long id, Integer position) {}

  public record ScenarioQuestionItem(
      Long id,
      Long stageId,
      Integer branchOrder,
      String title,
      Source sourceKind,
      Long attributeId,
      Input inputKind,
      ScenarioInputConfig inputConfig) {}

  public record ScenarioTransitionItem(
      Long id,
      Long fromQuestionId,
      Integer priority,
      ScenarioPredicate predicate,
      Target targetKind,
      Long targetQuestionId) {}
}
