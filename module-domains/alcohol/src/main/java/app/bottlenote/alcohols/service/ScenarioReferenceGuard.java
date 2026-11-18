package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.SCENARIO_REFERENCE_IN_USE;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioReferenceGuard {
  private final AlcoholScenarioRepository repository;
  private final AlcoholScenarioVersionRepository versions;
  private final AlcoholScenarioQuestionRepository questions;
  private final TastingTagGroupRepository tastingGroups;
  private final TastingTagRepository tastingTags;

  public void requireCompatible(AlcoholAttribute old, AlcoholAttribute candidate, Long groupId) {
    boolean changed =
        !Objects.equals(old.getGroupId(), groupId)
            || old.getDatatype() != candidate.getDatatype()
            || old.getAttributeType() != candidate.getAttributeType()
            || !Objects.equals(old.getUnit(), candidate.getUnit())
            || old.isAllowMultipleValues() != candidate.isAllowMultipleValues()
            || !Objects.equals(old.getAllowedValues(), candidate.getAllowedValues())
            || !Objects.equals(old.getPattern(), candidate.getPattern());
    if (changed) {
      requireUnpublished(List.of(old.getId()));
      reviseDrafts(List.of(old.getId()), false);
    }
  }

  public void beforeAttributeDeletion(Collection<Long> ids) {
    requireUnpublished(ids);
    reviseDrafts(ids, true);
  }

  private void requireUnpublished(Collection<Long> ids) {
    if (questions.hasPublishedAttributeReferences(ids))
      throw new ScenarioException(SCENARIO_REFERENCE_IN_USE, "속성 ID: " + ids);
  }

  private void reviseDrafts(Collection<Long> ids, boolean deleting) {
    for (var v : versions.findDraftsReferencingAttributes(ids)) {
      versions.claimVersion(v, v.getLockVersion());
      if (deleting
          && questions.findQuestions(v.getId()).stream()
              .anyMatch(
                  q ->
                      ids.contains(q.getAttributeId()) && q.getId().equals(v.getStartQuestionId())))
        v.startAt(null);
    }
    repository.flush();
  }

  public void requireMutableTastingGroup(String code) {
    tastingGroups.lockTastingGroups(List.of(code));
    if (questions.hasPublishedTastingGroupReferences(List.of(code)))
      throw new ScenarioException(SCENARIO_REFERENCE_IN_USE, "맛 그룹 코드: " + code);
  }

  public void beforeTastingGroupTagsAddition(String code, Collection<Long> tagIds) {
    tastingTags.lockTastingTags(tagIds);
    requireMutableTastingGroup(code);
  }

  public void beforeTastingTagDeletion(Long tagId) {
    tastingTags.lockTastingTags(List.of(tagId));
    var codes = tastingGroups.findTastingGroupCodesByTagId(tagId);
    tastingGroups.lockTastingGroups(codes);
    if (questions.hasPublishedTastingGroupReferences(codes))
      throw new ScenarioException(SCENARIO_REFERENCE_IN_USE, "맛 그룹의 태그 ID: " + tagId);
  }

  public void requireEmptyGroup(Long id) {
    if (repository.hasScenarios(id))
      throw new ScenarioException(SCENARIO_REFERENCE_IN_USE, "그룹의 시나리오를 먼저 삭제하거나 보존해야 합니다.");
  }
}
