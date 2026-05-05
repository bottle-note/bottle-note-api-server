package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Distillery;
import app.bottlenote.alcohols.domain.DistilleryRepository;
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryDistilleryRepository implements DistilleryRepository {

  private final List<Distillery> distilleries = new ArrayList<>();

  @Override
  public Optional<Distillery> findById(Long id) {
    return distilleries.stream().filter(d -> Objects.equals(d.getId(), id)).findFirst();
  }

  @Override
  public Page<AdminDistilleryItem> findAllDistilleries(String keyword, Pageable pageable) {
    List<AdminDistilleryItem> filtered =
        distilleries.stream()
            .filter(
                d ->
                    keyword == null
                        || keyword.isEmpty()
                        || d.getKorName().contains(keyword)
                        || d.getEngName().contains(keyword))
            .map(this::toAdminDistilleryItem)
            .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AdminDistilleryItem> pageContent =
        start < filtered.size() ? filtered.subList(start, end) : List.of();

    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  @Override
  public Distillery save(Distillery distillery) {
    Long id = distillery.getId();
    if (Objects.isNull(id)) {
      Long newId = (long) (distilleries.size() + 1);
      ReflectionTestUtils.setField(distillery, "id", newId);
    }
    final Long distilleryId = distillery.getId();
    distilleries.removeIf(d -> Objects.equals(d.getId(), distilleryId));
    distilleries.add(distillery);
    return distillery;
  }

  @Override
  public void delete(Distillery distillery) {
    distilleries.removeIf(d -> Objects.equals(d.getId(), distillery.getId()));
  }

  @Override
  public boolean existsByKorName(String korName) {
    return distilleries.stream().anyMatch(d -> Objects.equals(d.getKorName(), korName));
  }

  @Override
  public boolean existsByEngName(String engName) {
    return distilleries.stream().anyMatch(d -> Objects.equals(d.getEngName(), engName));
  }

  @Override
  public boolean existsByKorNameAndIdNot(String korName, Long id) {
    return distilleries.stream()
        .anyMatch(d -> Objects.equals(d.getKorName(), korName) && !Objects.equals(d.getId(), id));
  }

  @Override
  public boolean existsByEngNameAndIdNot(String engName, Long id) {
    return distilleries.stream()
        .anyMatch(d -> Objects.equals(d.getEngName(), engName) && !Objects.equals(d.getId(), id));
  }

  public void clear() {
    distilleries.clear();
  }

  private AdminDistilleryItem toAdminDistilleryItem(Distillery d) {
    return new AdminDistilleryItem(
        d.getId(),
        d.getKorName(),
        d.getEngName(),
        d.getImageUrl(),
        d.getCreateAt(),
        d.getLastModifyAt());
  }
}
