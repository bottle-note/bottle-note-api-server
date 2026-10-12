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
import app.bottlenote.alcohols.constant.ValueProvider;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.domain.TastingTag;
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
import app.bottlenote.alcohols.dto.response.TastingTagAlcoholCountItem;
import app.bottlenote.alcohols.exception.AlcoholException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminTastingTagGroupService implements AdminTastingTagGroupService {

  private final TastingTagGroupRepository tastingTagGroupRepository;
  private final TastingTagRepository tastingTagRepository;
  private final AlcoholsTastingTagsRepository alcoholsTastingTagsRepository;

  @Override
  @Transactional(readOnly = true)
  public AdminTastingTagGroupListResponse list() {
    List<TastingTagGroup> groups = tastingTagGroupRepository.findAllWithTags();
    Map<Long, String> names = tagNames();
    Map<Long, Long> counts = alcoholCounts(tagIdsOf(groups));

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
  public AdminTastingTagGroupItem get(String code) {
    TastingTagGroup group = requireGroup(code);
    return toItem(group, tagNames(), alcoholCounts(tagIdsOf(group)));
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
    TastingTagGroup saved = tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_CREATED, saved.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse update(String code, AdminTastingTagGroupUpdateRequest request) {
    TastingTagGroup group = requireGroup(code);
    ensureNameAvailable(request.name(), group.getId());
    group.update(request.name(), request.family(), request.description(), request.displayOrder());
    tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_UPDATED, group.getId());
  }

  @Override
  @Transactional
  public AdminResultResponse delete(String code) {
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
    TastingTagGroup group = requireGroup(code);
    Set<Long> knownIds = tagNames().keySet();
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
    TastingTagGroup group = requireGroup(code);
    if (!group.removeTag(tagId)) {
      throw new AlcoholException(TASTING_TAG_GROUP_TAG_NOT_FOUND);
    }
    tastingTagGroupRepository.save(group);
    return AdminResultResponse.of(TASTING_TAG_GROUP_TAG_REMOVED, group.getId());
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

  private Map<Long, String> tagNames() {
    Map<Long, String> names = new HashMap<>();
    for (TastingTag tag : tastingTagRepository.findAll()) {
      if (tag.getId() != null) {
        names.put(tag.getId(), tag.getKorName());
      }
    }
    return names;
  }

  /** 빈 목록은 IN () SQL을 만들지 않는다. 매핑이 없는 태그는 0으로 채운다. */
  private Map<Long, Long> alcoholCounts(Collection<Long> tastingTagIds) {
    if (tastingTagIds == null || tastingTagIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> counts = new HashMap<>();
    for (TastingTagAlcoholCountItem item :
        alcoholsTastingTagsRepository.countAlcoholsByTastingTagIds(tastingTagIds)) {
      counts.put(item.tastingTagId(), item.alcoholCount() == null ? 0L : item.alcoholCount());
    }
    return counts;
  }

  private Set<Long> tagIdsOf(List<TastingTagGroup> groups) {
    Set<Long> ids = new HashSet<>();
    for (TastingTagGroup group : groups) {
      ids.addAll(tagIdsOf(group));
    }
    return ids;
  }

  private Set<Long> tagIdsOf(TastingTagGroup group) {
    Set<Long> ids = new HashSet<>();
    for (TastingTagGroupTag tag : group.getTags()) {
      ids.add(tag.getTastingTagId());
    }
    return ids;
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
                    label(tag.getRole()),
                    label(tag.getRole().getProvider()),
                    counts.getOrDefault(tag.getTastingTagId(), 0L)))
        .toList();
  }

  private AdminCodeLabelItem label(TastingTagGroupRole role) {
    return new AdminCodeLabelItem(
        role.name(), role.getKorName(), role.getEngName(), role.getDescription());
  }

  private AdminCodeLabelItem label(ValueProvider provider) {
    return new AdminCodeLabelItem(
        provider.name(), provider.getKorName(), provider.getEngName(), provider.getDescription());
  }
}
