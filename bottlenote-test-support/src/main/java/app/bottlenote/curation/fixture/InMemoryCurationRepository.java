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

  private static final Comparator<Curation> EXPOSURE_START_DATE_ORDER =
      Comparator.comparing(
              Curation::getExposureStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(Curation::getId, Comparator.reverseOrder());

  private final Map<Long, Curation> database = new HashMap<>();

  @Override
  public Optional<Curation> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<Curation> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc() {
    return database.values().stream()
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .sorted(EXPOSURE_START_DATE_ORDER)
        .toList();
  }

  @Override
  public List<Curation> findAllVisibleOn(LocalDate today) {
    return database.values().stream()
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .filter(curation -> isVisibleOn(curation, today))
        .sorted(EXPOSURE_START_DATE_ORDER)
        .toList();
  }

  @Override
  public List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria) {
    if (criteria.specIds().isEmpty()) {
      return List.of();
    }

    return database.values().stream()
        .filter(curation -> Boolean.TRUE.equals(curation.getIsActive()))
        .filter(curation -> isVisibleOn(curation, criteria.today()))
        .filter(curation -> criteria.specIds().contains(curation.getSpecId()))
        .filter(curation -> matchesKeyword(curation, criteria))
        .filter(curation -> afterCursor(curation, criteria))
        .sorted(EXPOSURE_START_DATE_ORDER)
        .map(Curation::getId)
        .limit(criteria.fetchSize())
        .toList();
  }

  private static boolean afterCursor(Curation curation, CurationFeedSearchCriteria criteria) {
    if (criteria.lastId() == null) {
      return true;
    }
    LocalDate lastExposureStartDate = criteria.lastExposureStartDate();
    LocalDate exposureStartDate = curation.getExposureStartDate();
    if (lastExposureStartDate == null) {
      return exposureStartDate == null && curation.getId() < criteria.lastId();
    }
    return exposureStartDate == null
        || exposureStartDate.isBefore(lastExposureStartDate)
        || (exposureStartDate.equals(lastExposureStartDate) && curation.getId() < criteria.lastId());
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
            .sorted(EXPOSURE_START_DATE_ORDER)
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
