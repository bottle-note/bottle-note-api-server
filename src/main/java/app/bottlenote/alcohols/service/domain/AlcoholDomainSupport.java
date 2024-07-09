package app.bottlenote.alcohols.service.domain;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import java.util.Optional;

public interface AlcoholDomainSupport {

	/**
	 * AlcoholInfo를 반환하는 메서드입니다.
	 *
	 * @param alcoholId
	 * @param currentUserId
	 * @return Optional<AlcoholInfo>
	 */
	Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long currentUserId);

	/**
	 * 데이터베이스에 존재하는 Alcohol인지 검증하는 메서드입니다.
	 *
	 * @param alcoholId
	 * @return Boolean
	 */
	Boolean existsByAlcoholId(Long alcoholId);

	/**
	 * alcohol이 존재하지 않으면 예외를 던지는 메서드입니다.
	 *
	 * @param alcoholId
	 */
	void isValidAlcoholId(Long alcoholId);
}
