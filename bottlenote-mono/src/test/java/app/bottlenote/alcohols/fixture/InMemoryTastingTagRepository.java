package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    List<AdminTastingTagItem> filtered = tags.stream()
        .filter(t -> keyword == null || keyword.isEmpty()
            || t.getKorName().contains(keyword)
            || t.getEngName().contains(keyword))
        .map(t -> new AdminTastingTagItem(
            t.getId(), t.getKorName(), t.getEngName(),
            t.getIcon(), t.getDescription(),
            t.getCreateAt(), t.getLastModifyAt()))
        .toList();

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), filtered.size());
    List<AdminTastingTagItem> pageContent = start < filtered.size()
        ? filtered.subList(start, end)
        : List.of();

    return new PageImpl<>(pageContent, pageable, filtered.size());
  }

  public TastingTag save(TastingTag tag) {
    Long id = tag.getId();
    if (Objects.isNull(id)) {
      id = (long) (tags.size() + 1);
      ReflectionTestUtils.setField(tag, "id", id);
    }
    tags.add(tag);
    return tag;
  }

  public void clear() {
    tags.clear();
  }
}
