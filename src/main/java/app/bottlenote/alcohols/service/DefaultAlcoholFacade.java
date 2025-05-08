package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.core.structure.Pair;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static java.lang.Boolean.FALSE;

@FacadeService
@RequiredArgsConstructor
public class DefaultAlcoholFacade implements AlcoholFacade {
	private final AlcoholQueryRepository alcoholQueryRepository;

	@Override
	public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {
		return alcoholQueryRepository.findAlcoholInfoById(alcoholId, userId);
	}

	@Override
	public Pair<AlcoholSummaryItem, AlcoholSummaryItem> getAlcoholSummaryItemWithNext(Long alcoholId) {

		AlcoholSummaryItem firstItem
				= alcoholQueryRepository.findAlcoholInfoById(alcoholId, null)
				.orElse(null);

		if (firstItem == null) {
			return new Pair<>(null, null);
		}

		AlcoholSummaryItem secondItem
				= alcoholQueryRepository.findAlcoholInfoById(alcoholId - 1, null)
				.orElse(null);

		return Pair.of(firstItem, secondItem);
	}

	@Override
	public Boolean existsByAlcoholId(Long alcoholId) {
		return alcoholQueryRepository.existsByAlcoholId(alcoholId);
	}

	@Override
	public void isValidAlcoholId(Long alcoholId) {
		if (existsByAlcoholId(alcoholId).equals(FALSE)) {
			throw new AlcoholException(ALCOHOL_NOT_FOUND);
		}
	}

	@Override
	public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
		isValidAlcoholId(alcoholId);
		return alcoholQueryRepository.findById(alcoholId)
				.map(Alcohol::getImageUrl);
	}
}
