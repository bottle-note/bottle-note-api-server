package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.alcohols.repository.CustomAlcoholQueryRepository.AdminAlcoholDetailProjection;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAlcoholQueryRepository implements AlcoholQueryRepository {

  private final Map<Long, Alcohol> alcohols = new HashMap<>();

  @Override
  public Alcohol save(Alcohol alcohol) {
    Long id = alcohol.getId();
    if (Objects.isNull(id)) {
      id = (long) (alcohols.size() + 1);
      ReflectionTestUtils.setField(alcohol, "id", id);
    }
    alcohols.put(id, alcohol);
    return alcohol;
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
    return Optional.empty();
  }

  @Override
  public List<CategoryItem> findAllCategories(AlcoholType type) {
    return List.of();
  }

  @Override
  public List<Pair<String, String>> findAllCategoryPairs() {
    return alcohols.values().stream()
        .map(a -> Pair.of(a.getKorCategory(), a.getEngCategory()))
        .distinct()
        .toList();
  }

  @Override
  public Boolean existsByAlcoholId(Long alcoholId) {
    return alcohols.containsKey(alcoholId);
  }

  @Override
  public Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer size) {
    return null;
  }

  @Override
  public Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    return new PageImpl<>(List.of());
  }

  @Override
  public Optional<AdminAlcoholDetailProjection> findAdminAlcoholDetailById(Long alcoholId) {
    return Optional.empty();
  }
}
