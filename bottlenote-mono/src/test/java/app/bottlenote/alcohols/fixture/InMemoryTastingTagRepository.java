package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryTastingTagRepository implements TastingTagRepository {

  private final List<TastingTag> tags = new ArrayList<>();

  @Override
  public List<TastingTag> findAll() {
    return List.copyOf(tags);
  }

  @Override
  public Page<AdminTastingTagItem> findAllTastingTags(String keyword, Pageable pageable) {
    List<AdminTastingTagItem> filtered =
        tags.stream()
            .filter(
                t ->
                    keyword == null
                        || keyword.isEmpty()
                        || t.getKorName().contains(keyword)
                        || t.getEngName().contains(keyword))
            .map(this::toAdminTastingTagItem)
            .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AdminTastingTagItem> pageContent =
        start < filtered.size() ? filtered.subList(start, end) : List.of();

    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  @Override
  public Optional<TastingTag> findById(Long id) {
    return tags.stream().filter(t -> Objects.equals(t.getId(), id)).findFirst();
  }

  @Override
  public Optional<TastingTag> findByKorName(String korName) {
    return tags.stream().filter(t -> Objects.equals(t.getKorName(), korName)).findFirst();
  }

  @Override
  public List<TastingTag> findByParentId(Long parentId) {
    return tags.stream().filter(t -> Objects.equals(t.getParentId(), parentId)).toList();
  }

  @Override
  public TastingTag save(TastingTag tag) {
    Long id = tag.getId();
    if (Objects.isNull(id)) {
      Long newId = (long) (tags.size() + 1);
      ReflectionTestUtils.setField(tag, "id", newId);
    }
    final Long tagId = tag.getId();
    tags.removeIf(t -> Objects.equals(t.getId(), tagId));
    tags.add(tag);
    return tag;
  }

  @Override
  public void delete(TastingTag tag) {
    tags.removeIf(t -> Objects.equals(t.getId(), tag.getId()));
  }

  @Override
  public boolean existsByKorNameAndIdNot(String korName, Long id) {
    return tags.stream()
        .anyMatch(t -> Objects.equals(t.getKorName(), korName) && !Objects.equals(t.getId(), id));
  }

  @Override
  public boolean existsByParentId(Long parentId) {
    return tags.stream().anyMatch(t -> Objects.equals(t.getParentId(), parentId));
  }

  public void clear() {
    tags.clear();
  }

  private AdminTastingTagItem toAdminTastingTagItem(TastingTag tag) {
    return new AdminTastingTagItem(
        tag.getId(),
        tag.getKorName(),
        tag.getEngName(),
        tag.getIcon(),
        tag.getDescription(),
        tag.getParentId(),
        tag.getCreateAt(),
        tag.getLastModifyAt());
  }
}
