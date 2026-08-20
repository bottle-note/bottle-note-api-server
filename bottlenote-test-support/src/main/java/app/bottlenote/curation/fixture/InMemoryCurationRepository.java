package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import app.bottlenote.curation.dto.request.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCurationRepository implements CurationRepository {

  private final Map<Long, Curation> database = new HashMap<>();

  @Override
  public Optional<Curation> findById(Long id) { return Optional.ofNullable(database.get(id)); }

  @Override
  public List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc() {
    return database.values().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive()))
        .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId)).toList();
  }

  @Override
  public List<Curation> findAllVisibleOn(LocalDate today) {
    return database.values().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).filter(c -> isVisibleOn(c, today))
        .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId)).toList();
  }

  @Override
  public List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria) {
    if (criteria.specIds().isEmpty()) return List.of();
    Comparator<Curation> comparator = comparator(criteria.sortType(), criteria.sortOrder());
    return database.values().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).filter(c -> isVisibleOn(c, criteria.today()))
        .filter(c -> criteria.specIds().contains(c.getSpecId())).filter(c -> matchesKeyword(c, criteria))
        .filter(c -> afterCursor(c, criteria, comparator)).sorted(comparator).map(Curation::getId).limit(criteria.fetchSize()).toList();
  }

  private static boolean afterCursor(Curation curation, CurationFeedSearchCriteria criteria, Comparator<Curation> comparator) {
    if (criteria.lastId() == null) return true;
    Curation cursor = Curation.builder().id(criteria.lastId()).exposureStartDate(criteria.lastExposureStartDate())
        .displayOrder(criteria.lastDisplayOrder()).build();
    return comparator.compare(curation, cursor) > 0;
  }

  private static Comparator<Curation> comparator(CurationSortType sortType, SortOrder sortOrder) {
    Comparator<Long> ids = sortOrder == SortOrder.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
    if (sortType == CurationSortType.DISPLAY_ORDER) {
      Comparator<Integer> primary = sortOrder == SortOrder.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
      return Comparator.comparing(Curation::getDisplayOrder, primary).thenComparing(Curation::getId, ids);
    }
    Comparator<LocalDate> primary = sortOrder == SortOrder.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder();
    return Comparator.comparing(Curation::getExposureStartDate, Comparator.nullsLast(primary)).thenComparing(Curation::getId, ids);
  }

  @Override public List<Curation> findAllByIdIn(Collection<Long> ids) { return database.values().stream().filter(c -> ids.contains(c.getId())).toList(); }
  @Override public Optional<Curation> findVisibleById(Long id, LocalDate today) { return findById(id).filter(c -> Boolean.TRUE.equals(c.getIsActive())).filter(c -> isVisibleOn(c, today)); }

  @Override
  public Page<Curation> searchForAdmin(String keyword, Long specId, Boolean isActive, Pageable pageable, CurationSortType sortType, SortOrder sortOrder) {
    List<Curation> all = database.values().stream().filter(c -> keyword == null || keyword.isBlank() || c.getName().contains(keyword))
        .filter(c -> specId == null || c.getSpecId().equals(specId)).filter(c -> isActive == null || c.getIsActive().equals(isActive))
        .sorted(comparator(sortType, sortOrder)).toList();
    int start = (int) pageable.getOffset(), end = Math.min(start + pageable.getPageSize(), all.size());
    return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
  }

  @Override public Curation save(Curation curation) { Long id = curation.getId(); if (id == null) { id = database.size() + 1L; ReflectionTestUtils.setField(curation, "id", id); } database.put(id, curation); return curation; }
  @Override public void delete(Curation curation) { database.remove(curation.getId()); }
  private boolean isVisibleOn(Curation c, LocalDate today) { return (c.getExposureStartDate() == null || !c.getExposureStartDate().isAfter(today)) && (c.getExposureEndDate() == null || !c.getExposureEndDate().isBefore(today)); }
  private boolean matchesKeyword(Curation c, CurationFeedSearchCriteria criteria) { if (criteria.keyword() == null || criteria.keyword().isBlank()) return true; String keyword = criteria.keyword().trim(); return contains(c.getName(), keyword) || contains(c.getDescription(), keyword) || criteria.keywordMatchedSpecIds().contains(c.getSpecId()); }
  private boolean contains(String value, String keyword) { return value != null && value.contains(keyword); }
}
