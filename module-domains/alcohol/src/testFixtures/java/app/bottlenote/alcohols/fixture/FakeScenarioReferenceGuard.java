package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.service.ScenarioReferenceGuard;
import java.util.Collection;
import java.util.List;

/** 게시/초안 참조가 없는 속성·태그 단위 테스트용 대역이다. */
public class FakeScenarioReferenceGuard extends ScenarioReferenceGuard {
  private final TastingTagGroupRepository groups;
  private final TastingTagRepository tags;

  public FakeScenarioReferenceGuard(TastingTagGroupRepository groups, TastingTagRepository tags) {
    super(null, null, null, groups, tags);
    this.groups = groups;
    this.tags = tags;
  }

  @Override
  public void requireCompatible(AlcoholAttribute old, AlcoholAttribute candidate, Long groupId) {}

  @Override
  public void beforeAttributeDeletion(Collection<Long> ids) {}

  @Override
  public void requireMutableTastingGroup(String code) {
    groups.lockTastingGroups(List.of(code));
  }

  @Override
  public void beforeTastingTagDeletion(Long tagId) {
    tags.lockTastingTags(List.of(tagId));
    groups.lockTastingGroups(groups.findTastingGroupCodesByTagId(tagId));
  }

  @Override
  public void requireEmptyGroup(Long id) {}
}
