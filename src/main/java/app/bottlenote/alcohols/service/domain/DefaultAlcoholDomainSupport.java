package app.bottlenote.alcohols.service.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAlcoholDomainSupport implements AlcoholDomainSupport {

	private final AlcoholQueryRepository alcoholQueryRepository;

	@Override
	public AlcoholInfo findAlcoholInfoById(Long alcoholId, Long userId) {
		return alcoholQueryRepository.findAlcoholInfoById(alcoholId, userId);
	}

	@Override
	public void existsByAlcoholId(Long alcoholId) {
		if (alcoholQueryRepository.existsByAlcoholId(alcoholId).equals(Boolean.FALSE)) {
			throw new AlcoholException((ALCOHOL_NOT_FOUND));
		}
	}


	@Override
	public Optional<Alcohol> findById(Long alcoholId) {
		return alcoholQueryRepository.findById(alcoholId);
	}
}
