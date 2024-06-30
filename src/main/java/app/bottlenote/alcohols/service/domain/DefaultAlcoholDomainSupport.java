package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.review.dto.response.AlcoholInfo;
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
	public Boolean existsByAlcoholId(Long alcoholId) {
		return alcoholQueryRepository.existsByAlcoholId(alcoholId);
	}


	@Override
	public Optional<Alcohol> findById(Long alcoholId) {
		return alcoholQueryRepository.findById(alcoholId);
	}
}
