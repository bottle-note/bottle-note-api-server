package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.exception.*;
import java.util.*;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholScenarioRepository implements AlcoholScenarioRepository {
  private final Map<Long,AlcoholAttributeScenario> scenarios=new LinkedHashMap<>();
  private final Map<Long,AlcoholAttributeScenarioVersion> versions=new LinkedHashMap<>();
  private final Map<Long,AlcoholAttributeScenarioStage> stages=new LinkedHashMap<>();
  private final Map<Long,AlcoholAttributeScenarioQuestion> questions=new LinkedHashMap<>();
  private final Map<Long,AlcoholAttributeScenarioTransition> transitions=new LinkedHashMap<>();
  private long sequence;
  private final TastingTagGroupRepository tastingGroups;

  public InMemoryAlcoholScenarioRepository() {
    this(new InMemoryTastingTagGroupRepository());
  }

  public InMemoryAlcoholScenarioRepository(TastingTagGroupRepository tastingGroups) {
    this.tastingGroups = tastingGroups;
  }
  private <T> T save(T value,Map<Long,T> rows) {
    Long id=(Long)ReflectionTestUtils.getField(value,"id");
    if(id==null) { id=++sequence;ReflectionTestUtils.setField(value,"id",id); }
    rows.put(id,value);return value;
  }
  @Override public List<AlcoholAttributeScenario> findScenarios(Long groupId) { return scenarios.values().stream().filter(s -> s.getGroupId().equals(groupId)).sorted(Comparator.comparingInt(AlcoholAttributeScenario::getSlot)).toList(); }
  @Override public Optional<AlcoholAttributeScenario> findScenario(Long id) { return Optional.ofNullable(scenarios.get(id)); }
  @Override public Optional<AlcoholAttributeScenarioVersion> findVersion(Long id) { return Optional.ofNullable(versions.get(id)); }
  @Override public List<AlcoholAttributeScenarioVersion> findVersions(Long id) { return versions.values().stream().filter(v -> v.getScenarioId().equals(id)).sorted(Comparator.comparingInt(AlcoholAttributeScenarioVersion::getVersionNo)).toList(); }
  @Override public List<AlcoholAttributeScenarioStage> findStages(Long id) { return stages.values().stream().filter(s -> s.getVersionId().equals(id)).sorted(Comparator.comparingInt(AlcoholAttributeScenarioStage::getPosition)).toList(); }
  @Override public List<AlcoholAttributeScenarioQuestion> findQuestions(Long id) { return questions.values().stream().filter(q -> q.getVersionId().equals(id)).toList(); }
  @Override public List<AlcoholAttributeScenarioTransition> findTransitions(Long id) { return transitions.values().stream().filter(t -> t.getVersionId().equals(id)).toList(); }
  @Override public AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario s) {
    if(findScenarios(s.getGroupId()).stream().anyMatch(old -> !Objects.equals(old.getId(),s.getId()) && old.getSlot()==s.getSlot())) throw new ScenarioException(ScenarioExceptionCode.SCENARIO_SLOT_OCCUPIED);
    return save(s,scenarios);
  }
  @Override public AlcoholAttributeScenarioVersion saveVersion(AlcoholAttributeScenarioVersion v) { return save(v,versions); }
  @Override public AlcoholAttributeScenarioStage saveStage(AlcoholAttributeScenarioStage s) { return save(s,stages); }
  @Override public AlcoholAttributeScenarioQuestion saveQuestion(AlcoholAttributeScenarioQuestion q) { return save(q,questions); }
  @Override public AlcoholAttributeScenarioTransition saveTransition(AlcoholAttributeScenarioTransition t) { return save(t,transitions); }
  private synchronized void claim(Object row,long expected) {
    long actual=(long)ReflectionTestUtils.getField(row,"lockVersion");
    if(actual!=expected) throw new ScenarioException(ScenarioExceptionCode.SCENARIO_CONFLICT);
    ReflectionTestUtils.setField(row,"lockVersion",actual+1);
  }
  @Override public void claimScenario(AlcoholAttributeScenario s,long expected) { claim(s,expected); }
  @Override public void claimVersion(AlcoholAttributeScenarioVersion v,long expected) { claim(v,expected); }
  @Override public void deleteScenario(AlcoholAttributeScenario s) { new ArrayList<>(findVersions(s.getId())).forEach(this::deleteVersion);scenarios.remove(s.getId()); }
  @Override public void deleteVersion(AlcoholAttributeScenarioVersion v) { deleteTransitions(findTransitions(v.getId()));deleteQuestions(findQuestions(v.getId()));deleteStages(findStages(v.getId()));versions.remove(v.getId()); }
  @Override public void deleteStages(List<AlcoholAttributeScenarioStage> values) { values.forEach(s -> stages.remove(s.getId())); }
  @Override public void deleteQuestions(List<AlcoholAttributeScenarioQuestion> values) {
    var ids=values.stream().map(AlcoholAttributeScenarioQuestion::getId).toList();
    transitions.values().removeIf(t -> ids.contains(t.getFromQuestionId()) || ids.contains(t.getTargetQuestionId()));
    ids.forEach(questions::remove);
  }
  @Override public void deleteTransitions(List<AlcoholAttributeScenarioTransition> values) { values.forEach(t -> transitions.remove(t.getId())); }
  @Override public boolean hasScenarios(Long groupId) { return !findScenarios(groupId).isEmpty(); }
  @Override public boolean hasPublishedAttributeReferences(Collection<Long> ids) { return questions.values().stream().anyMatch(q -> ids.contains(q.getAttributeId()) && versions.get(q.getVersionId()).getStatus()==Status.PUBLISHED); }
  @Override public List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(Collection<Long> ids) { return versions.values().stream().filter(v -> v.getStatus()==Status.DRAFT && findQuestions(v.getId()).stream().anyMatch(q -> ids.contains(q.getAttributeId()))).toList(); }
  @Override public List<TastingTagGroup> lockTastingGroups(Collection<String> codes) {
    return tastingGroups.findAllWithTags().stream().filter(g -> codes.contains(g.getCode())).toList();
  }
  @Override public void lockTastingTags(Collection<Long> ids) {}
  @Override public List<String> findTastingGroupCodesByTagId(Long id) {
    return tastingGroups.findAllWithTags().stream().filter(g -> g.getTags().stream().anyMatch(t -> t.getTastingTagId().equals(id))).map(TastingTagGroup::getCode).toList();
  }
  @Override public boolean hasPublishedTastingGroupReference(String code) { return questions.values().stream().filter(q -> versions.get(q.getVersionId()).getStatus()==Status.PUBLISHED && q.getSourceKind()==Source.TASTING_GROUP).anyMatch(q -> q.getInputConfig().choices().stream().anyMatch(c -> code.equals(c.value().asText()))); }
  @Override public void flush() {}
}
