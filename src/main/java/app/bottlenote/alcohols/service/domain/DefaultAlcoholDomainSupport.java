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
	public AlcoholInfo findAlcoholById(Long alcoholId, Long userId) {
		return alcoholQueryRepository.findAlcoholById(alcoholId, userId);
	}

	@Override
	public Optional<Alcohol> findById(Long alcoholId) {
		return alcoholQueryRepository.findById(alcoholId);
	}
}
