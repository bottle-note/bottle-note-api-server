package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest.*;
import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.*;
import java.util.*;

final class ScenarioDefinitionMapper {
  private ScenarioDefinitionMapper() {}

  static ScenarioItem item(AlcoholAttributeScenario s) {
    return new ScenarioItem(
        s.getId(),
        s.getGroupId(),
        s.getSlot(),
        s.getTitle(),
        s.isEnabled(),
        s.getActiveVersionId(),
        s.getLockVersion(),
        s.getCreateAt(),
        s.getLastModifyAt(),
        AlcoholAttributeResponseMapper.toPrincipal(s.getCreatePrincipal()),
        AlcoholAttributeResponseMapper.toPrincipal(s.getLastModifyPrincipal()));
  }

  static ScenarioVersionItem item(AlcoholAttributeScenarioVersion v) {
    return new ScenarioVersionItem(
        v.getId(),
        v.getVersionNo(),
        v.getStatus(),
        v.getLockVersion(),
        v.getPublishedAt(),
        v.getCreateAt(),
        v.getLastModifyAt(),
        AlcoholAttributeResponseMapper.toPrincipal(v.getCreatePrincipal()),
        AlcoholAttributeResponseMapper.toPrincipal(v.getLastModifyPrincipal()));
  }

  static ScenarioDefinitionRequest definition(
      AlcoholAttributeScenarioVersion v, AlcoholScenarioRepository repo) {
    var stages = repo.findStages(v.getId());
    Map<Long, Integer> positions = new HashMap<>();
    stages.forEach(s -> positions.put(s.getId(), s.getPosition()));
    return new ScenarioDefinitionRequest(
        v.getDefinitionSchemaVersion(),
        v.getStartQuestionId(),
        stages.stream().map(s -> new ScenarioStageItem(s.getId(), s.getPosition())).toList(),
        repo.findQuestions(v.getId()).stream()
            .sorted(
                Comparator.comparing(
                        (AlcoholAttributeScenarioQuestion q) -> positions.get(q.getStageId()))
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
        repo.findTransitions(v.getId()).stream()
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

  static ScenarioDefinitionRequest forClone(ScenarioDefinitionRequest d) {
    return new ScenarioDefinitionRequest(
        1,
        d.startQuestionId() == null ? null : -d.startQuestionId(),
        d.stages().stream().map(s -> new ScenarioStageItem(-s.id(), s.position())).toList(),
        d.questions().stream()
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
        d.transitions().stream()
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
