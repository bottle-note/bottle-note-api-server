package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.review.dto.response.AlcoholInfo;
import java.util.Optional;

public interface AlcoholDomainSupport {

	AlcoholInfo findAlcoholById(Long alcoholId, Long userId);

	Optional<Alcohol> findById(Long alcoholId);

}
