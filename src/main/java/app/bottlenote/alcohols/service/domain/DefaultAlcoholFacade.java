package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static java.lang.Boolean.FALSE;

@Service
@RequiredArgsConstructor
public class DefaultAlcoholFacade implements AlcoholFacade {

	private final AlcoholQueryRepository alcoholQueryRepository;

	@Override
	public Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long userId) {
		return alcoholQueryRepository.findAlcoholInfoById(alcoholId, userId);
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
