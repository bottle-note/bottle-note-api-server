package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryTastingTagRepository implements TastingTagRepository {

  private final List<TastingTag> tags = new ArrayList<>();

  @Override
  public List<TastingTag> findAll() {
    return List.copyOf(tags);
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
