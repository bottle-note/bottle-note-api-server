package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.dto.response.AdminTastingTagItem;
import java.util.List;

public interface TastingTagRepository {

  List<TastingTag> findAll();

  List<AdminTastingTagItem> findAllTastingTags();
}
