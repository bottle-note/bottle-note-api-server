package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.shared.alcohols.constant.AlcoholType;
import app.bottlenote.shared.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.shared.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.shared.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.shared.alcohols.dto.response.CategoryItem;
import app.bottlenote.shared.alcohols.payload.AlcoholSummaryItem;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public class InMemoryAlcoholQueryRepository implements AlcoholQueryRepository {

  private final Map<Long, Alcohol> alcohols = new HashMap<>();

  @Override
  public Alcohol save(Alcohol alcohol) {
    return alcohols.put(alcohol.getId(), alcohol);
  }

  @Override
  public Optional<Alcohol> findById(Long alcoholId) {
    return Optional.ofNullable(alcohols.get(alcoholId));
  }

  @Override
  public List<Alcohol> findAll() {
    return List.copyOf(alcohols.values());
  }

  @Override
  public List<Alcohol> findAllByIdIn(List<Long> ids) {
    return alcohols.values().stream().filter(alcohol -> ids.contains(alcohol.getId())).toList();
  }

  @Override
  public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto) {
    return null;
  }

  @Override
  public AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long AlcoholId) {
    return null;
  }

  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {
    return null;
  }

  @Override
  public List<CategoryItem> findAllCategories(AlcoholType type) {
    return List.of();
  }

  @Override
  public Boolean existsByAlcoholId(Long alcoholId) {
    return null;
  }

  @Override
  public Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer size) {
    return null;
  }
}
