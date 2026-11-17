package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import java.util.List;

public record ScenarioDefinition(
    Integer definitionSchemaVersion,
    Long startQuestionId,
    List<ScenarioStageItem> stages,
    List<ScenarioQuestionItem> questions,
    List<ScenarioTransitionItem> transitions) {
  public static ScenarioDefinition empty() {
    return new ScenarioDefinition(1, null, List.of(), List.of(), List.of());
  }

  public record ScenarioStageItem(Long id, Integer position) {}

  public record ScenarioQuestionItem(
      Long id,
      Long stageId,
      Integer branchOrder,
      String title,
      AlcoholQuestionSource sourceKind,
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

  public ScenarioDefinition forClone() {
    return new ScenarioDefinition(
        1,
        this.startQuestionId() == null ? null : -this.startQuestionId(),
        this.stages().stream().map(s -> new ScenarioStageItem(-s.id(), s.position())).toList(),
        this.questions().stream()
            .map(
                q ->
                    new ScenarioQuestionItem(
                        -q.id(),
                        -q.stageId(),
                        q.branchOrder(),
                        q.title(),
                        q.sourceKind(),
                        q.attributeId(),
                        q.inputKind(),
                        q.inputConfig()))
            .toList(),
        this.transitions().stream()
            .map(
                t ->
                    new ScenarioTransitionItem(
                        -t.id(),
                        -t.fromQuestionId(),
                        t.priority(),
                        t.predicate(),
                        t.targetKind(),
                        t.targetQuestionId() == null ? null : -t.targetQuestionId()))
            .toList());
  }
}
