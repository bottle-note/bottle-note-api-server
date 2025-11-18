package app.bottlenote.alcohols.domain;

import java.util.List;
import java.util.Optional;

public interface PopularAlcoholRepository {

  PopularAlcohol save(PopularAlcohol popularAlcohol);

  Optional<PopularAlcohol> findById(Long id);

  List<PopularAlcohol> findAll();

  void delete(PopularAlcohol popularAlcohol);
}
