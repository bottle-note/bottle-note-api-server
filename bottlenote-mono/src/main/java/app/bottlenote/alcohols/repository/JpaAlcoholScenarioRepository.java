package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.common.domain.AuditPrincipal;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class JpaAlcoholScenarioRepository implements AlcoholScenarioRepository {
  private final EntityManager em;
  private final AuditorAware<AuditPrincipal> auditor;

  @Override
  public List<AlcoholAttributeScenario> findScenarios(Long groupId) {
    return em.createQuery(
            "select s from alcohol_attribute_scenario s where s.groupId=:id order by s.slot",
            AlcoholAttributeScenario.class)
        .setParameter("id", groupId)
        .getResultList();
  }

  @Override
  public Optional<AlcoholAttributeScenario> findScenario(Long id) {
    return Optional.ofNullable(em.find(AlcoholAttributeScenario.class, id));
  }

  @Override
  public Optional<AlcoholAttributeScenarioVersion> findVersion(Long id) {
    return Optional.ofNullable(em.find(AlcoholAttributeScenarioVersion.class, id));
  }

  @Override
  public List<AlcoholAttributeScenarioVersion> findVersions(Long id) {
    return em.createQuery(
            "select v from alcohol_attribute_scenario_version v where v.scenarioId=:id order by v.versionNo",
            AlcoholAttributeScenarioVersion.class)
        .setParameter("id", id)
        .getResultList();
  }

  @Override
  public List<AlcoholAttributeScenarioStage> findStages(Long id) {
    return em.createQuery(
            "select s from alcohol_attribute_scenario_stage s where s.versionId=:id order by s.position",
            AlcoholAttributeScenarioStage.class)
        .setParameter("id", id)
        .getResultList();
  }

  @Override
  public List<AlcoholAttributeScenarioQuestion> findQuestions(Long id) {
    return em.createQuery(
            "select q from alcohol_attribute_scenario_question q where q.versionId=:id order by q.stageId,q.branchOrder",
            AlcoholAttributeScenarioQuestion.class)
        .setParameter("id", id)
        .getResultList();
  }

  @Override
  public List<AlcoholAttributeScenarioTransition> findTransitions(Long id) {
    return em.createQuery(
            "select t from alcohol_attribute_scenario_transition t where t.versionId=:id order by t.fromQuestionId,t.priority",
            AlcoholAttributeScenarioTransition.class)
        .setParameter("id", id)
        .getResultList();
  }

  @Override
  public AlcoholAttributeScenario saveScenario(AlcoholAttributeScenario s) {
    try {
      return persist(s);
    } catch (PersistenceException | DataIntegrityViolationException ex) {
      if (AlcoholAttributeConstraintViolation.isDuplicate(ex, Set.of("uk_scenarios_group_slot")))
        throw new ScenarioException(SCENARIO_SLOT_OCCUPIED);
      throw ex;
    }
  }

  @Override
  public AlcoholAttributeScenarioVersion saveVersion(AlcoholAttributeScenarioVersion v) {
    return persist(v);
  }

  @Override
  public AlcoholAttributeScenarioStage saveStage(AlcoholAttributeScenarioStage s) {
    return persist(s);
  }

  @Override
  public AlcoholAttributeScenarioQuestion saveQuestion(AlcoholAttributeScenarioQuestion q) {
    return persist(q);
  }

  @Override
  public AlcoholAttributeScenarioTransition saveTransition(AlcoholAttributeScenarioTransition t) {
    return persist(t);
  }

  private <T> T persist(T entity) {
    if (!em.contains(entity)) em.persist(entity);
    em.flush();
    return entity;
  }

  @Override
  public void claimScenario(AlcoholAttributeScenario s, long expected) {
    claim("alcohol_attribute_scenario", s.getId(), expected);
    em.refresh(s);
  }

  @Override
  public void claimVersion(AlcoholAttributeScenarioVersion v, long expected) {
    claim("alcohol_attribute_scenario_version", v.getId(), expected);
    em.refresh(v);
  }

  private void claim(String entity, Long id, long expected) {
    int changed =
        em.createQuery(
                "update "
                    + entity
                    + " e set e.lockVersion=e.lockVersion+1, e.lastModifyAt=:now, e.lastModifyPrincipal=:principal where e.id=:id and e.lockVersion=:expected")
            .setParameter("now", LocalDateTime.now())
            .setParameter("principal", auditor.getCurrentAuditor().orElse(null))
            .setParameter("id", id)
            .setParameter("expected", expected)
            .executeUpdate();
    if (changed != 1) throw new ScenarioException(SCENARIO_CONFLICT);
  }

  @Override
  public void deleteScenario(AlcoholAttributeScenario s) {
    em.remove(s);
    em.flush();
  }

  @Override
  public void deleteVersion(AlcoholAttributeScenarioVersion v) {
    em.remove(v);
    em.flush();
  }

  @Override
  public void deleteStages(List<AlcoholAttributeScenarioStage> rows) {
    rows.forEach(em::remove);
    em.flush();
  }

  @Override
  public void deleteQuestions(List<AlcoholAttributeScenarioQuestion> rows) {
    rows.forEach(em::remove);
    em.flush();
  }

  @Override
  public void deleteTransitions(List<AlcoholAttributeScenarioTransition> rows) {
    rows.forEach(em::remove);
    em.flush();
  }

  @Override
  public boolean hasScenarios(Long id) {
    return em.createQuery(
                "select count(s) from alcohol_attribute_scenario s where s.groupId=:id", Long.class)
            .setParameter("id", id)
            .getSingleResult()
        > 0;
  }

  @Override
  public boolean hasPublishedAttributeReferences(Collection<Long> ids) {
    if (ids.isEmpty()) return false;
    return em.createQuery(
                "select count(q) from alcohol_attribute_scenario_question q, alcohol_attribute_scenario_version v where q.versionId=v.id and v.status=:status and q.attributeId in :ids",
                Long.class)
            .setParameter("status", Status.PUBLISHED)
            .setParameter("ids", ids)
            .getSingleResult()
        > 0;
  }

  @Override
  public List<AlcoholAttributeScenarioVersion> findDraftsReferencingAttributes(
      Collection<Long> ids) {
    if (ids.isEmpty()) return List.of();
    return em.createQuery(
            "select distinct v from alcohol_attribute_scenario_version v, alcohol_attribute_scenario_question q where q.versionId=v.id and v.status=:status and q.attributeId in :ids order by v.id",
            AlcoholAttributeScenarioVersion.class)
        .setParameter("status", Status.DRAFT)
        .setParameter("ids", ids)
        .getResultList();
  }

  @Override
  public List<TastingTagGroup> lockTastingGroups(Collection<String> codes) {
    if (codes.isEmpty()) return List.of();
    return em.createQuery(
            "select g from tasting_tag_group g where g.code in :codes order by g.id",
            TastingTagGroup.class)
        .setParameter("codes", codes)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultList();
  }

  @Override
  public void lockTastingTags(Collection<Long> ids) {
    if (ids.isEmpty()) return;
    em.createQuery("select t from tasting_tag t where t.id in :ids order by t.id", TastingTag.class)
        .setParameter("ids", ids)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultList();
  }

  @Override
  public List<String> findTastingGroupCodesByTagId(Long tagId) {
    return em.createQuery(
            "select g.code from tasting_tag_group g join g.tags t where t.tastingTagId=:id order by g.id",
            String.class)
        .setParameter("id", tagId)
        .getResultList();
  }

  @Override
  public boolean hasPublishedTastingGroupReference(String code) {
    var questions =
        em.createQuery(
                "select q from alcohol_attribute_scenario_question q, alcohol_attribute_scenario_version v where q.versionId=v.id and v.status=:status and q.sourceKind=:source",
                AlcoholAttributeScenarioQuestion.class)
            .setParameter("status", Status.PUBLISHED)
            .setParameter("source", Source.TASTING_GROUP)
            .getResultList();
    return questions.stream()
        .anyMatch(
            q ->
                q.getInputConfig().choices() != null
                    && q.getInputConfig().choices().stream()
                        .anyMatch(c -> c.value() != null && code.equals(c.value().asText())));
  }

  @Override
  public void flush() {
    em.flush();
  }
}
