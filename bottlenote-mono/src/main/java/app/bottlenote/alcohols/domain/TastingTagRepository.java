package app.bottlenote.alcohols.domain;

import java.util.List;

public interface TastingTagRepository {

  List<TastingTag> findAll();
}
