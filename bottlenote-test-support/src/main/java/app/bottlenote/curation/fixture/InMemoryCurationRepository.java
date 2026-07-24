package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
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
  public Optional<Curation> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc() {
    return database.values().stream()
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId))
        .toList();
  }

  @Override
  public List<Curation> findAllVisibleOn(LocalDate today) {
    return database.values().stream()
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .filter(curation -> isVisibleOn(curation, today))
        .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId))
        .toList();
  }

  @Override
  public List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria) {
    if (criteria.specIds().isEmpty()) {
      return List.of();
    }

    List<Long> candidateIds =
        database.values().stream()
            .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
            .filter(curation -> isVisibleOn(curation, criteria.today()))
            .filter(curation -> criteria.specIds().contains(curation.getSpecId()))
            .filter(curation -> matchesKeyword(curation, criteria))
            .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId))
            .map(Curation::getId)
            .toList();
    int start = (int) Math.min(criteria.offset(), candidateIds.size());
    int end = Math.min(start + criteria.fetchSize(), candidateIds.size());
    return candidateIds.subList(start, end);
  }

  @Override
  public List<Curation> findAllByIdIn(Collection<Long> ids) {
    return database.values().stream().filter(curation -> ids.contains(curation.getId())).toList();
  }

  @Override
  public Optional<Curation> findVisibleById(Long id, LocalDate today) {
    return findById(id)
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .filter(curation -> isVisibleOn(curation, today));
  }

  @Override
  public Page<Curation> searchForAdmin(String keyword, Long specId, Boolean isActive, Pageable pageable) {
    List<Curation> all =
        database.values().stream()
            .filter(
                curation ->
                    keyword == null || keyword.isBlank() || curation.getName().contains(keyword))
            .filter(curation -> specId == null || curation.getSpecId().equals(specId))
            .filter(curation -> isActive == null || curation.getIsActive().equals(isActive))
            .sorted(Comparator.comparing(Curation::getDisplayOrder).thenComparing(Curation::getId))
            .toList();
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), all.size());
    List<Curation> content = start < all.size() ? all.subList(start, end) : List.of();
    return new PageImpl<>(content, pageable, all.size());
  }

  @Override
  public Curation save(Curation curation) {
    Long id = curation.getId();
    if (id == null) {
      id = database.size() + 1L;
      ReflectionTestUtils.setField(curation, "id", id);
    }
    database.put(id, curation);
    return curation;
  }

  @Override
  public void delete(Curation curation) {
    database.remove(curation.getId());
  }

  private boolean isVisibleOn(Curation curation, LocalDate today) {
    return (curation.getExposureStartDate() == null
            || !curation.getExposureStartDate().isAfter(today))
        && (curation.getExposureEndDate() == null
            || !curation.getExposureEndDate().isBefore(today));
  }

  private boolean matchesKeyword(Curation curation, CurationFeedSearchCriteria criteria) {
    if (criteria.keyword() == null || criteria.keyword().isBlank()) {
      return true;
    }
    String keyword = criteria.keyword().trim();
    return contains(curation.getName(), keyword)
        || contains(curation.getDescription(), keyword)
        || criteria.keywordMatchedSpecIds().contains(curation.getSpecId());
  }

  private boolean contains(String value, String keyword) {
    return value != null && value.contains(keyword);
  }
}
