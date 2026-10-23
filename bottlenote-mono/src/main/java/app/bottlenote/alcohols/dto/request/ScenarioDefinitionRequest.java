package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioInputConfig;
import app.bottlenote.alcohols.domain.ScenarioPredicate;
import java.util.List;

public record ScenarioDefinitionRequest(
    Integer definitionSchemaVersion,
    Long startQuestionId,
    List<ScenarioStageItem> stages,
    List<ScenarioQuestionItem> questions,
    List<ScenarioTransitionItem> transitions) {
  public ScenarioDefinition toDefinition() {
    return new ScenarioDefinition(
        definitionSchemaVersion,
        startQuestionId,
        stages == null
            ? null
            : stages.stream()
                .map(
                    s ->
                        s == null
                            ? null
                            : new ScenarioDefinition.ScenarioStageItem(s.id(), s.position()))
                .toList(),
        questions == null
            ? null
            : questions.stream()
                .map(
                    q ->
                        q == null
                            ? null
                            : new ScenarioDefinition.ScenarioQuestionItem(
                                q.id(),
                                q.stageId(),
                                q.branchOrder(),
                                q.title(),
                                q.sourceKind(),
                                q.attributeId(),
                                q.inputKind(),
                                q.inputConfig()))
                .toList(),
        transitions == null
            ? null
            : transitions.stream()
                .map(
                    t ->
                        t == null
                            ? null
                            : new ScenarioDefinition.ScenarioTransitionItem(
                                t.id(),
                                t.fromQuestionId(),
                                t.priority(),
                                t.predicate(),
                                t.targetKind(),
                                t.targetQuestionId()))
                .toList());
  }

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
