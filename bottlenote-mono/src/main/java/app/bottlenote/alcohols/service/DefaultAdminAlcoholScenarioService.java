package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;
import static app.bottlenote.alcohols.service.ScenarioDefinitionMapper.*;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest;
import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.*;
import app.bottlenote.alcohols.exception.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultAdminAlcoholScenarioService implements AdminAlcoholScenarioService {
  private final AlcoholScenarioRepository repository;
  private final AlcoholAttributeGroupRepository groupRepository;
  private final AlcoholAttributeRepository attributeRepository;
  private final TastingTagGroupRepository tastingGroupRepository;
  private final ScenarioDefinitionValidator validator;
  private final ScenarioPreviewService previewService;

  @Override
  @Transactional(readOnly = true)
  public List<ScenarioItem> findAll(Long groupId) {
    requireGroup(groupId, false);
    return repository.findScenarios(groupId).stream().map(ScenarioDefinitionMapper::item).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ScenarioDetail getDetail(Long groupId, Long id) {
    return detail(requireScenario(groupId, id));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDetail create(Long groupId, Create request) {
    requireGroup(groupId, true);
    if (repository.findScenarios(groupId).stream().anyMatch(s -> s.getSlot() == request.slot()))
      throw new ScenarioException(SCENARIO_SLOT_OCCUPIED);
    var scenario =
        repository.saveScenario(
            AlcoholAttributeScenario.create(groupId, request.slot(), request.title()));
    repository.saveVersion(AlcoholAttributeScenarioVersion.create(scenario.getId(), 1));
    return detail(scenario);
  }

  @Override
  @Transactional
  public ScenarioItem update(Long groupId, Long id, Update request) {
    var s = requireScenario(groupId, id);
    repository.claimScenario(s, request.expectedLockVersion());
    s.update(request.title(), request.enabled());
    repository.flush();
    return item(s);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioVersionDetail createDraft(Long groupId, Long id, Draft request) {
    var s = requireScenario(groupId, id);
    ScenarioDefinitionRequest copy = null;
    if (request.sourceVersionId() != null) {
      var source = requireVersion(s, request.sourceVersionId());
      if (source.getStatus() != Status.PUBLISHED)
        throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE, "복제 원본은 게시 버전이어야 합니다.");
      copy = forClone(definition(source, repository));
      lockSources(copy);
    }
    repository.claimScenario(s, request.expectedScenarioLockVersion());
    int number =
        repository.findVersions(id).stream()
                .mapToInt(AlcoholAttributeScenarioVersion::getVersionNo)
                .max()
                .orElse(0)
            + 1;
    var v = repository.saveVersion(AlcoholAttributeScenarioVersion.create(id, number));
    if (copy != null) {
      validator.validate(copy, groupId, lockSources(copy), tastingCodes(), false);
      storeGraph(v, copy);
    }
    return versionDetail(v);
  }

  @Override
  @Transactional(readOnly = true)
  public ScenarioVersionDetail getVersion(Long groupId, Long id, Long versionId) {
    return versionDetail(requireVersion(requireScenario(groupId, id), versionId));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioVersionDetail save(Long groupId, Long id, Long versionId, Save request) {
    var s = requireScenario(groupId, id);
    var v = requireVersion(s, versionId);
    requireDraft(v);
    var attributes = lockSources(request.definition());
    validator.validate(request.definition(), groupId, attributes, tastingCodes(), false);
    repository.claimVersion(v, request.expectedLockVersion());
    requireDraft(v);
    storeGraph(v, request.definition());
    return versionDetail(v);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDetail publish(Long groupId, Long id, Long versionId, Publish request) {
    var s = requireScenario(groupId, id);
    var v = requireVersion(s, versionId);
    requireDraft(v);
    var d = definition(v, repository);
    var attributes = lockSources(d);
    repository.claimScenario(s, request.expectedScenarioLockVersion());
    repository.claimVersion(v, request.expectedLockVersion());
    requireDraft(v);
    d = definition(v, repository);
    validator.validate(d, groupId, attributes, tastingCodes(), true);
    v.publish();
    s.activate(v.getId());
    repository.flush();
    return detail(s);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDeleted delete(Long groupId, Long id, long expected) {
    var s = requireScenario(groupId, id);
    repository.claimScenario(s, expected);
    var versions = repository.findVersions(id);
    if (versions.stream().anyMatch(v -> v.getStatus() == Status.PUBLISHED))
      throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE);
    s.activate(null);
    for (var v : versions) v.startAt(null);
    repository.flush();
    repository.deleteScenario(s);
    return new ScenarioDeleted(id);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDeleted deleteDraft(
      Long groupId, Long id, Long versionId, long scenarioExpected, long versionExpected) {
    var s = requireScenario(groupId, id);
    var v = requireVersion(s, versionId);
    repository.claimScenario(s, scenarioExpected);
    repository.claimVersion(v, versionExpected);
    requireDraft(v);
    if (Objects.equals(s.getActiveVersionId(), v.getId())) s.activate(null);
    v.startAt(null);
    repository.flush();
    repository.deleteVersion(v);
    return new ScenarioDeleted(versionId);
  }

  @Override
  @Transactional(readOnly = true)
  public ScenarioPreview preview(
      Long groupId, Long id, Long versionId, AdminScenarioPreviewRequest request) {
    var v = requireVersion(requireScenario(groupId, id), versionId);
    if (v.getLockVersion() != request.expectedLockVersion())
      throw new ScenarioException(SCENARIO_CONFLICT);
    var d = definition(v, repository);
    var ids =
        d.questions().stream()
            .map(q -> q.attributeId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var attributes =
        attributeRepository.findAllByIdIn(ids).stream()
            .collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity()));
    validator.validate(d, groupId, attributes, tastingCodes(), true);
    return previewService.preview(groupId, d, attributes, tastingCodes(), request);
  }

  private Map<Long, AlcoholAttribute> lockSources(ScenarioDefinitionRequest d) {
    if (d == null || d.questions() == null) throw ScenarioException.invalid("질문 배열이 필요합니다.");
    var ids =
        d.questions().stream()
            .filter(Objects::nonNull)
            .map(q -> q.attributeId())
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    var attributes =
        ids.isEmpty() ? List.<AlcoholAttribute>of() : attributeRepository.lockByIds(ids);
    var codes =
        d.questions().stream()
            .filter(Objects::nonNull)
            .filter(
                q ->
                    q.sourceKind() == Source.TASTING_GROUP
                        && q.inputConfig() != null
                        && q.inputConfig().choices() != null)
            .flatMap(q -> q.inputConfig().choiceCodes().stream())
            .distinct()
            .sorted()
            .toList();
    repository.lockTastingGroups(codes);
    return attributes.stream()
        .collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity()));
  }

  private Set<String> tastingCodes() {
    return tastingGroupRepository.findAllWithTags().stream()
        .map(TastingTagGroup::getCode)
        .collect(Collectors.toSet());
  }

  private void requireGroup(Long groupId, boolean lock) {
    boolean exists =
        lock
            ? !groupRepository.lockByIds(List.of(groupId)).isEmpty()
            : groupRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .anyMatch(g -> g.getId().equals(groupId));
    if (!exists) throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND);
  }

  private AlcoholAttributeScenario requireScenario(Long groupId, Long id) {
    return repository
        .findScenario(id)
        .filter(s -> s.getGroupId().equals(groupId))
        .orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
  }

  private AlcoholAttributeScenarioVersion requireVersion(AlcoholAttributeScenario s, Long id) {
    return repository
        .findVersion(id)
        .filter(v -> v.getScenarioId().equals(s.getId()))
        .orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
  }

  private static void requireDraft(AlcoholAttributeScenarioVersion v) {
    if (v.getStatus() != Status.DRAFT) throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE);
  }

  private ScenarioDetail detail(AlcoholAttributeScenario s) {
    return new ScenarioDetail(
        item(s),
        repository.findVersions(s.getId()).stream().map(ScenarioDefinitionMapper::item).toList());
  }

  private ScenarioVersionDetail versionDetail(AlcoholAttributeScenarioVersion v) {
    return new ScenarioVersionDetail(item(v), definition(v, repository));
  }

  private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
    return list.stream().collect(Collectors.toMap(id, Function.identity()));
  }

  private static void requireOwned(Collection<Long> requested, Set<Long> existing, String kind) {
    for (Long id : requested)
      if (id > 0 && !existing.contains(id))
        throw ScenarioException.invalid(kind + " " + id + ": 이 버전이 소유한 ID가 아닙니다.");
  }

  private void storeGraph(AlcoholAttributeScenarioVersion v, ScenarioDefinitionRequest d) {
    var stages = byId(repository.findStages(v.getId()), AlcoholAttributeScenarioStage::getId);
    var questions =
        byId(repository.findQuestions(v.getId()), AlcoholAttributeScenarioQuestion::getId);
    var rules =
        byId(repository.findTransitions(v.getId()), AlcoholAttributeScenarioTransition::getId);
    requireOwned(d.stages().stream().map(x -> x.id()).toList(), stages.keySet(), "단계");
    requireOwned(d.questions().stream().map(x -> x.id()).toList(), questions.keySet(), "질문");
    requireOwned(d.transitions().stream().map(x -> x.id()).toList(), rules.keySet(), "규칙");
    v.startAt(null);
    repository.flush();
    int n = 10000;
    for (var s : stages.values()) s.moveTo(n++);
    for (var q : questions.values()) q.moveTo(n++);
    for (var t : rules.values()) t.moveTo(n++);
    repository.flush();
    Map<Long, Long> stageIds = new HashMap<>(), questionIds = new HashMap<>();
    for (var s : d.stages()) {
      var entity =
          s.id() > 0
              ? stages.get(s.id())
              : AlcoholAttributeScenarioStage.create(v.getId(), s.position());
      entity.moveTo(s.position());
      repository.saveStage(entity);
      stageIds.put(s.id(), entity.getId());
    }
    for (var q : d.questions()) {
      var entity =
          q.id() > 0 ? questions.get(q.id()) : AlcoholAttributeScenarioQuestion.create(v.getId());
      entity.configure(
          stageIds.get(q.stageId()),
          q.branchOrder(),
          q.title(),
          q.sourceKind(),
          q.attributeId(),
          q.inputKind(),
          q.inputConfig());
      repository.saveQuestion(entity);
      questionIds.put(q.id(), entity.getId());
    }
    var keptRules = d.transitions().stream().map(t -> t.id()).collect(Collectors.toSet());
    repository.deleteTransitions(
        rules.values().stream().filter(t -> !keptRules.contains(t.getId())).toList());
    for (var t : d.transitions()) {
      var entity =
          t.id() > 0 ? rules.get(t.id()) : AlcoholAttributeScenarioTransition.create(v.getId());
      entity.configure(
          questionIds.get(t.fromQuestionId()),
          t.priority(),
          t.predicate(),
          t.targetKind(),
          questionIds.get(t.targetQuestionId()));
      repository.saveTransition(entity);
    }
    var keptQuestions = new HashSet<>(questionIds.values());
    repository.deleteQuestions(
        questions.values().stream().filter(q -> !keptQuestions.contains(q.getId())).toList());
    var keptStages = new HashSet<>(stageIds.values());
    repository.deleteStages(
        stages.values().stream().filter(s -> !keptStages.contains(s.getId())).toList());
    v.startAt(questionIds.get(d.startQuestionId()));
    repository.flush();
  }
}
