package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.review.dto.response.AlcoholInfo;
import java.util.Optional;

public interface AlcoholDomainSupport {

	Optional<Alcohol> findById(Long alcoholId);

	AlcoholInfo findAlcoholInfoById(Long alcoholId, Long currentUserId);

	Boolean existsByAlcoholId(Long alcoholId);
}
