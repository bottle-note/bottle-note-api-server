package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioInputConfig;
import app.bottlenote.alcohols.domain.ScenarioPredicate;
import java.util.List;

public record ScenarioDefinitionResponse(
    Integer definitionSchemaVersion,
    Long startQuestionId,
    List<DefinitionStageItem> stages,
    List<DefinitionQuestionItem> questions,
    List<DefinitionTransitionItem> transitions) {
  public static ScenarioDefinitionResponse from(ScenarioDefinition d) {
    return new ScenarioDefinitionResponse(
        d.definitionSchemaVersion(),
        d.startQuestionId(),
        d.stages().stream().map(s -> new DefinitionStageItem(s.id(), s.position())).toList(),
        d.questions().stream()
            .map(
                q ->
                    new DefinitionQuestionItem(
                        q.id(),
                        q.stageId(),
                        q.branchOrder(),
                        q.title(),
                        q.sourceKind(),
                        q.attributeId(),
                        q.inputKind(),
                        q.inputConfig()))
            .toList(),
        d.transitions().stream()
            .map(
                t ->
                    new DefinitionTransitionItem(
                        t.id(),
                        t.fromQuestionId(),
                        t.priority(),
                        t.predicate(),
                        t.targetKind(),
                        t.targetQuestionId()))
            .toList());
  }

  public record DefinitionStageItem(Long id, Integer position) {}

  public record DefinitionQuestionItem(
      Long id,
      Long stageId,
      Integer branchOrder,
      String title,
      Source sourceKind,
      Long attributeId,
      Input inputKind,
      ScenarioInputConfig inputConfig) {}

  public record DefinitionTransitionItem(
      Long id,
      Long fromQuestionId,
      Integer priority,
      ScenarioPredicate predicate,
      Target targetKind,
      Long targetQuestionId) {}
}
