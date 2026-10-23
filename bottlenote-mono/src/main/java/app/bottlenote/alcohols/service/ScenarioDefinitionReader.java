package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioDefinitionReader {
  private final AlcoholScenarioStageRepository stages;
  private final AlcoholScenarioQuestionRepository questions;
  private final AlcoholScenarioTransitionRepository transitions;

  public ScenarioDefinition read(AlcoholAttributeScenarioVersion v) {
    var stageRows = stages.findStages(v.getId());
    Map<Long, Integer> positions = new HashMap<>();
    stageRows.forEach(s -> positions.put(s.getId(), s.getPosition()));
    return new ScenarioDefinition(
        v.getDefinitionSchemaVersion(),
        v.getStartQuestionId(),
        stageRows.stream().map(s -> new ScenarioStageItem(s.getId(), s.getPosition())).toList(),
        questions.findQuestions(v.getId()).stream()
            .sorted(
                Comparator.comparing(
                        (AlcoholAttributeScenarioQuestion q) -> positions.get(q.getStageId()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(AlcoholAttributeScenarioQuestion::getBranchOrder))
            .map(
                q ->
                    new ScenarioQuestionItem(
                        q.getId(),
                        q.getStageId(),
                        q.getBranchOrder(),
                        q.getTitle(),
                        q.getSourceKind(),
                        q.getAttributeId(),
                        q.getInputKind(),
                        q.getInputConfig()))
            .toList(),
        transitions.findTransitions(v.getId()).stream()
            .map(
                t ->
                    new ScenarioTransitionItem(
                        t.getId(),
                        t.getFromQuestionId(),
                        t.getPriority(),
                        t.getPredicate(),
                        t.getTargetKind(),
                        t.getTargetQuestionId()))
            .toList());
  }
}
