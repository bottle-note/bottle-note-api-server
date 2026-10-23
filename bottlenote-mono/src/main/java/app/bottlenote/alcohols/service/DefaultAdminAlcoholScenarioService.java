package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;

import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*;
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
  private final AlcoholScenarioVersionRepository versions;
  private final ScenarioDefinitionStore definitions;
  private final AlcoholAttributeGroupRepository groupRepository;
  private final AlcoholAttributeRepository attributeRepository;
  private final TastingTagGroupRepository tastingGroupRepository;
  private final ScenarioDefinitionValidator validator;
  private final ScenarioPreviewService previewService;

  @Override
  @Transactional(readOnly = true)
  public List<ScenarioItem> findAll(Long groupId) {
    requireGroup(groupId, false);
    return repository.findScenarios(groupId).stream()
        .map(DefaultAdminAlcoholScenarioService::item)
        .toList();
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
    versions.save(AlcoholAttributeScenarioVersion.create(scenario.getId(), 1));
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
    ScenarioDefinition copy = null;
    Sources copySources = null;
    if (request.sourceVersionId() != null) {
      var source = requireVersion(s, request.sourceVersionId());
      if (source.getStatus() != Status.PUBLISHED)
        throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE, "복제 원본은 게시 버전이어야 합니다.");
      copy = definitions.read(source).forClone();
      copySources = lockSources(copy);
    }
    repository.claimScenario(s, request.expectedScenarioLockVersion());
    int number =
        versions.findVersions(id).stream()
                .mapToInt(AlcoholAttributeScenarioVersion::getVersionNo)
                .max()
                .orElse(0)
            + 1;
    var v = versions.save(AlcoholAttributeScenarioVersion.create(id, number));
    if (copy != null) {
      validator.validate(
          copy, groupId, copySources.attributes(), copySources.tastingCodes(), false);
      definitions.store(v, copy);
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
    var definition = request.definition();
    var sources = lockSources(definition);
    validator.validate(definition, groupId, sources.attributes(), sources.tastingCodes(), false);
    versions.claimVersion(v, request.expectedLockVersion());
    requireDraft(v);
    definitions.store(v, definition);
    return versionDetail(v);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDetail publish(Long groupId, Long id, Long versionId, Publish request) {
    var s = requireScenario(groupId, id);
    var v = requireVersion(s, versionId);
    requireDraft(v);
    if (v.getLockVersion() != request.expectedLockVersion())
      throw new ScenarioException(SCENARIO_CONFLICT);
    // 참조 잠금 순서를 유지하고 초기 revision과 CAS로 읽은 그래프의 일관성을 보장한다.
    var d = definitions.read(v);
    var sources = lockSources(d);
    repository.claimScenario(s, request.expectedScenarioLockVersion());
    versions.claimVersion(v, request.expectedLockVersion());
    requireDraft(v);
    validator.validate(d, groupId, sources.attributes(), sources.tastingCodes(), true);
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
    var versionRows = versions.findVersions(id);
    if (versionRows.stream().anyMatch(v -> v.getStatus() == Status.PUBLISHED))
      throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE);
    s.activate(null);
    for (var v : versionRows) v.startAt(null);
    repository.flush();
    repository.delete(s);
    return new ScenarioDeleted(id);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ScenarioDeleted deleteDraft(
      Long groupId, Long id, Long versionId, long scenarioExpected, long versionExpected) {
    var s = requireScenario(groupId, id);
    var v = requireVersion(s, versionId);
    repository.claimScenario(s, scenarioExpected);
    versions.claimVersion(v, versionExpected);
    requireDraft(v);
    if (Objects.equals(s.getActiveVersionId(), v.getId())) s.activate(null);
    v.startAt(null);
    repository.flush();
    versions.delete(v);
    return new ScenarioDeleted(versionId);
  }

  @Override
  @Transactional(readOnly = true)
  public ScenarioPreview preview(
      Long groupId, Long id, Long versionId, AdminScenarioPreviewRequest request) {
    var v = requireVersion(requireScenario(groupId, id), versionId);
    if (v.getLockVersion() != request.expectedLockVersion())
      throw new ScenarioException(SCENARIO_CONFLICT);
    var d = definitions.read(v);
    return previewService.preview(groupId, d, request);
  }

  private record Sources(Map<Long, AlcoholAttribute> attributes, Set<String> tastingCodes) {}

  private Sources lockSources(ScenarioDefinition d) {
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
                    q.sourceKind() == AlcoholQuestionSource.TASTING_GROUP
                        && q.inputConfig() != null
                        && q.inputConfig().choices() != null)
            .flatMap(q -> q.inputConfig().choiceCodes().stream())
            .distinct()
            .sorted()
            .toList();
    var groups = tastingGroupRepository.lockTastingGroups(codes);
    return new Sources(
        attributes.stream().collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity())),
        groups.stream().map(TastingTagGroup::getCode).collect(Collectors.toSet()));
  }

  private void requireGroup(Long groupId, boolean lock) {
    boolean exists =
        lock
            ? !groupRepository.lockByIds(List.of(groupId)).isEmpty()
            : groupRepository.existsById(groupId);
    if (!exists) throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND);
  }

  private AlcoholAttributeScenario requireScenario(Long groupId, Long id) {
    return repository
        .findById(id)
        .filter(s -> s.getGroupId().equals(groupId))
        .orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
  }

  private AlcoholAttributeScenarioVersion requireVersion(AlcoholAttributeScenario s, Long id) {
    return versions
        .findById(id)
        .filter(v -> v.getScenarioId().equals(s.getId()))
        .orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
  }

  private static void requireDraft(AlcoholAttributeScenarioVersion v) {
    if (v.getStatus() != Status.DRAFT) throw new ScenarioException(SCENARIO_PUBLISHED_IMMUTABLE);
  }

  private ScenarioDetail detail(AlcoholAttributeScenario s) {
    return new ScenarioDetail(
        item(s),
        versions.findVersions(s.getId()).stream()
            .map(DefaultAdminAlcoholScenarioService::item)
            .toList());
  }

  private ScenarioVersionDetail versionDetail(AlcoholAttributeScenarioVersion v) {
    return new ScenarioVersionDetail(item(v), definitions.read(v));
  }

  private static ScenarioItem item(AlcoholAttributeScenario s) {
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

  private static ScenarioVersionItem item(AlcoholAttributeScenarioVersion v) {
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
}
