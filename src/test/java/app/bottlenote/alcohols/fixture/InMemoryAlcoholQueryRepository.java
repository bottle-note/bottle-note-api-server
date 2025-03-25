package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.global.service.cursor.PageResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
		return alcohols.values().stream()
			.filter(alcohol -> ids.contains(alcohol.getId()))
			.toList();
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
}
