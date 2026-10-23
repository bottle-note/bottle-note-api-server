package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
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

  static ScenarioDefinition forClone(ScenarioDefinition d) {
    return new ScenarioDefinition(
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
