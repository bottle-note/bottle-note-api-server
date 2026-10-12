package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_NOT_FOUND;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.domain.AlcoholsTastingTagsRepository;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import app.bottlenote.alcohols.domain.TastingTagGroupTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.response.AdminCodeLabelItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagCatalogItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupRefItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagIconResponse;
import app.bottlenote.alcohols.dto.response.TastingTagAlcoholCountItem;
import app.bottlenote.alcohols.dto.response.TastingTagCatalogSourceItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminTastingTagCatalogService implements AdminTastingTagCatalogService {

  private static final Comparator<AdminTastingTagCatalogItem> BY_KOR_NAME =
      Comparator.comparing(
              AdminTastingTagCatalogItem::korName, Comparator.nullsLast(String::compareTo))
          .thenComparing(AdminTastingTagCatalogItem::id, Comparator.nullsLast(Long::compareTo));

  private static final Comparator<AdminTastingTagCatalogItem> BY_ALCOHOL_COUNT =
      Comparator.comparingLong(AdminTastingTagCatalogItem::alcoholCount)
          .reversed()
          .thenComparing(BY_KOR_NAME);

  private final TastingTagRepository tastingTagRepository;
  private final AlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  private final TastingTagGroupRepository tastingTagGroupRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AdminTastingTagCatalogItem> findAll() {
    return toItems(tastingTagRepository.findAllCatalogSources(), groupsByTagId()).stream()
        .sorted(BY_KOR_NAME)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminTastingTagIconResponse findIcon(Long id) {
    TastingTag tag =
        tastingTagRepository
            .findById(id)
            .orElseThrow(() -> new AlcoholException(TASTING_TAG_NOT_FOUND));
    return new AdminTastingTagIconResponse(tag.getIcon());
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminTastingTagCatalogItem> findUngrouped(String keyword) {
    String normalized = keyword == null ? "" : keyword.trim();
    Set<Long> groupedTagIds = groupedTagIds();
    List<TastingTagCatalogSourceItem> sources =
        tastingTagRepository.findAllCatalogSources().stream()
            .filter(source -> !groupedTagIds.contains(source.id()))
            .filter(source -> normalized.isEmpty() || contains(source.korName(), normalized))
            .toList();
    return toItems(sources, Map.of()).stream().sorted(BY_ALCOHOL_COUNT).toList();
  }

  private List<AdminTastingTagCatalogItem> toItems(
      List<TastingTagCatalogSourceItem> sources,
      Map<Long, List<AdminTastingTagGroupRefItem>> groupsByTagId) {
    Map<Long, Long> alcoholCounts =
        alcoholCounts(sources.stream().map(TastingTagCatalogSourceItem::id).toList());
    return sources.stream()
        .map(
            source ->
                new AdminTastingTagCatalogItem(
                    source.id(),
                    source.korName(),
                    source.engName(),
                    source.parentId(),
                    alcoholCounts.getOrDefault(source.id(), 0L),
                    List.copyOf(groupsByTagId.getOrDefault(source.id(), List.of()))))
        .toList();
  }

  private Map<Long, Long> alcoholCounts(Collection<Long> tagIds) {
    if (tagIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> counts = new HashMap<>();
    for (TastingTagAlcoholCountItem count :
        alcoholsTastingTagsRepository.countAlcoholsByTastingTagIds(tagIds)) {
      counts.put(count.tastingTagId(), count.alcoholCount());
    }
    return counts;
  }

  private Map<Long, List<AdminTastingTagGroupRefItem>> groupsByTagId() {
    Map<Long, List<AdminTastingTagGroupRefItem>> groups = new HashMap<>();
    for (TastingTagGroup group : tastingTagGroupRepository.findAllWithTags()) {
      for (TastingTagGroupTag tag : group.getTags()) {
        groups
            .computeIfAbsent(tag.getTastingTagId(), ignored -> new ArrayList<>())
            .add(
                new AdminTastingTagGroupRefItem(
                    group.getCode(), group.getName(), label(tag.getRole())));
      }
    }
    return groups;
  }

  private Set<Long> groupedTagIds() {
    Set<Long> tagIds = new HashSet<>();
    for (TastingTagGroup group : tastingTagGroupRepository.findAllWithTags()) {
      for (TastingTagGroupTag tag : group.getTags()) {
        tagIds.add(tag.getTastingTagId());
      }
    }
    return tagIds;
  }

  private static boolean contains(String korName, String keyword) {
    return korName != null && korName.contains(keyword);
  }

  private static AdminCodeLabelItem label(TastingTagGroupRole role) {
    return new AdminCodeLabelItem(
        role.name(), role.getKorName(), role.getEngName(), role.getDescription());
  }
}
