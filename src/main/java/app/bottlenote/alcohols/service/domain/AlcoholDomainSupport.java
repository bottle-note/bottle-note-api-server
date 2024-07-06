package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import java.util.Optional;

public interface AlcoholDomainSupport {

	Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long currentUserId);

	Boolean existsByAlcoholId(Long alcoholId);

	void isValidAlcoholId(Long alcoholId);
}
