package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_DUPLICATE_CODE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_DUPLICATE_NAME;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_HAS_TAGS;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_GROUP_TAG_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_TAG_ADDED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_TAG_REMOVED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_TAG_ROLE_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.TASTING_TAG_GROUP_UPDATED;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import app.bottlenote.alcohols.domain.TastingTagGroupTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagRoleRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagsRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupListResponse;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupTagItem;
import app.bottlenote.alcohols.dto.response.TastingTagCatalogSourceItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminTastingTagGroupService implements AdminTastingTagGroupService {

  private static final String DUPLICATE_NAME_KEY = "uk_tasting_tag_groups_name";

  private final TastingTagGroupRepository tastingTagGroupRepository;
  private final TastingTagRepository tastingTagRepository;
  private final AlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  private final ScenarioReferenceGuard scenarioReferenceGuard;

  @Override
  @Transactional(readOnly = true)
  public AdminTastingTagGroupListResponse findAll() {
    List<TastingTagGroup> groups = tastingTagGroupRepository.findAllWithTags();
    Set<Long> tagIds = new HashSet<>();
    for (TastingTagGroup group : groups) {
      tagIds.addAll(group.tagIds());
    }
    Map<Long, String> names = tagNames(tagIds);
    Map<Long, Long> counts = TastingTagAlcoholCounts.byTagId(alcoholsTastingTagsRepository, tagIds);

    List<AdminTastingTagGroupItem> items = new ArrayList<>();
    LinkedHashSet<String> families = new LinkedHashSet<>();
    for (TastingTagGroup group : groups) {
      if (group.getFamily() != null && !group.getFamily().isBlank()) {
        families.add(group.getFamily());
      }
      items.add(toItem(group, names, counts));
    }
    return new AdminTastingTagGroupListResponse(List.copyOf(items), List.copyOf(families));
  }

  @Override
  @Transactional(readOnly = true)
  public AdminTastingTagGroupItem getDetail(String code) {
    TastingTagGroup group = requireGroup(code);
    return toItem(
        group,
        tagNames(group.tagIds()),
        TastingTagAlcoholCounts.byTagId(alcoholsTastingTagsRepository, group.tagIds()));
  }

  @Override
  @Transactional
  public AdminResultResponse create(AdminTastingTagGroupCreateRequest request) {
    TastingTagGroup group =
        TastingTagGroup.create(
            request.code(),
            request.name(),
            request.family(),
            request.description(),
            request.displayOrder());
    if (tastingTagGroupRepository.existsByCode(group.getCode())) {
      throw new AlcoholException(TASTING_TAG_GROUP_DUPLICATE_CODE);
    }
    if (tastingTagGroupRepository.existsByName(group.getName())) {
      throw new AlcoholException(TASTING_TAG_GROUP_DUPLICATE_NAME);
    }
    TastingTagGroup saved = saveOrThrowDuplicate(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_CREATED, saved.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse update(String code, AdminTastingTagGroupUpdateRequest request) {
    TastingTagGroup group = requireGroup(code);
    ensureNameAvailable(request.name(), group.getId());
    group.update(request.name(), request.family(), request.description(), request.displayOrder());
    saveOrThrowDuplicate(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_UPDATED, group.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse delete(String code) {
    scenarioReferenceGuard.requireMutableTastingGroup(code);
    TastingTagGroup group = requireGroup(code);
    if (group.hasTags()) {
      throw new AlcoholException(TASTING_TAG_GROUP_HAS_TAGS);
    }
    tastingTagGroupRepository.delete(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_DELETED, group.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse addTags(String code, AdminTastingTagGroupTagsRequest request) {
    scenarioReferenceGuard.beforeTastingGroupTagsAddition(code, request.tastingTagIds());
    TastingTagGroup group = requireGroup(code);
    Set<Long> knownIds = allTagNames().keySet();
    for (Long tastingTagId : request.tastingTagIds()) {
      if (!knownIds.contains(tastingTagId)) {
        throw new AlcoholException(TASTING_TAG_NOT_FOUND);
      }
    }
    for (Long tastingTagId : request.tastingTagIds()) {
      group.putTag(tastingTagId, request.role());
    }
    tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_TAG_ADDED, group.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse changeTagRole(
      String code, Long tagId, AdminTastingTagGroupTagRoleRequest request) {
    scenarioReferenceGuard.requireMutableTastingGroup(code);
    TastingTagGroup group = requireGroup(code);
    if (group.findTag(tagId).isEmpty()) {
      throw new AlcoholException(TASTING_TAG_GROUP_TAG_NOT_FOUND);
    }
    group.putTag(tagId, request.role());
    tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_TAG_ROLE_UPDATED, group.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse removeTag(String code, Long tagId) {
    scenarioReferenceGuard.requireMutableTastingGroup(code);
    TastingTagGroup group = requireGroup(code);
    if (!group.removeTag(tagId)) {
      throw new AlcoholException(TASTING_TAG_GROUP_TAG_NOT_FOUND);
    }
    tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_TAG_REMOVED, group.getId());
  }

  /** 선검사 뒤의 경합은 유니크 키 이름으로 코드 중복과 이름 중복을 가른다. 저장은 즉시 flush 되어 여기서 잡힌다. */
  private TastingTagGroup saveOrThrowDuplicate(TastingTagGroup group) {
    try {
      return tastingTagGroupRepository.save(group);
    } catch (DataIntegrityViolationException ex) {
      throw new AlcoholException(duplicateOf(ex));
    }
  }

  private static AlcoholExceptionCode duplicateOf(DataIntegrityViolationException ex) {
    Throwable current = ex;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains(DUPLICATE_NAME_KEY)) {
        return TASTING_TAG_GROUP_DUPLICATE_NAME;
      }
      current = current.getCause();
    }
    return TASTING_TAG_GROUP_DUPLICATE_CODE;
  }

  private TastingTagGroup requireGroup(String code) {
    return tastingTagGroupRepository
        .findByCode(code)
        .orElseThrow(() -> new AlcoholException(TASTING_TAG_GROUP_NOT_FOUND));
  }

  /** 수정 전에 검사한다. id가 null인 NOT 비교는 JPA와 InMemory가 서로 다르다. */
  private void ensureNameAvailable(String name, Long excludeId) {
    if (name == null || name.isBlank()) {
      return;
    }
    if (tastingTagGroupRepository.existsByNameAndIdNot(name.trim(), excludeId)) {
      throw new AlcoholException(TASTING_TAG_GROUP_DUPLICATE_NAME);
    }
  }

  /** 아직 그룹에 없는 태그도 존재 확인이 필요해서 카탈로그 전체를 본다. */
  private Map<Long, String> allTagNames() {
    return tagNames(null);
  }

  /** ids가 null이면 카탈로그 전체, 아니면 그 ID만 남긴다. 아이콘 컬럼은 읽지 않는다. */
  private Map<Long, String> tagNames(Collection<Long> ids) {
    Set<Long> wanted = ids == null ? null : Set.copyOf(ids);
    if (wanted != null && wanted.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> names = new HashMap<>();
    for (TastingTagCatalogSourceItem source : tastingTagRepository.findAllCatalogSources()) {
      if (source.id() != null && (wanted == null || wanted.contains(source.id()))) {
        names.put(source.id(), source.korName());
      }
    }
    return names;
  }

  private AdminTastingTagGroupItem toItem(
      TastingTagGroup group, Map<Long, String> names, Map<Long, Long> counts) {
    return new AdminTastingTagGroupItem(
        group.getId(),
        group.getCode(),
        group.getName(),
        group.getFamily(),
        group.getDescription(),
        group.getDisplayOrder(),
        tagItems(group, names, counts));
  }

  /** 대표 태그를 먼저 두고, 그다음 한글 이름·태그 ID 순으로 안정 정렬한다. */
  private List<AdminTastingTagGroupTagItem> tagItems(
      TastingTagGroup group, Map<Long, String> names, Map<Long, Long> counts) {
    return group.getTags().stream()
        .sorted(
            Comparator.comparingInt(
                    (TastingTagGroupTag tag) -> tag.getRole() == TastingTagGroupRole.CORE ? 0 : 1)
                .thenComparing(
                    tag -> names.get(tag.getTastingTagId()),
                    Comparator.nullsLast(String::compareTo))
                .thenComparing(
                    TastingTagGroupTag::getTastingTagId, Comparator.nullsLast(Long::compareTo)))
        .map(
            tag ->
                new AdminTastingTagGroupTagItem(
                    tag.getTastingTagId(),
                    names.get(tag.getTastingTagId()),
                    AdminCodeLabelItem.of(tag.getRole()),
                    AdminCodeLabelItem.of(tag.getRole().getProvider()),
                    counts.getOrDefault(tag.getTastingTagId(), 0L)))
        .toList();
  }
}
