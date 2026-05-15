package app.bottlenote.curation.fixture;

import app.bottlenote.curation.domain.Curation;
import app.bottlenote.curation.domain.CurationRepository;
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
  public Page<Curation> searchForAdmin(String keyword, Boolean isActive, Pageable pageable) {
    List<Curation> all =
        database.values().stream()
            .filter(
                curation ->
                    keyword == null || keyword.isBlank() || curation.getName().contains(keyword))
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
}
