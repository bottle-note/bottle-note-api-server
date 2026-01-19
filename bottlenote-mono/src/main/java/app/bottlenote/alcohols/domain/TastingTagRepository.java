package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TastingTagRepository {

  List<TastingTag> findAll();

  Page<AdminTastingTagItem> findAllTastingTags(String keyword, Pageable pageable);
}
