package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.AlcoholSummaryItem;

import java.util.Optional;

public interface AlcoholFacade {


	/**
	 * AlcoholInfo를 반환하는 메서드입니다.
	 *
	 * @param alcoholId
	 * @param currentUserId
	 * @return Optional<AlcoholSummaryItem>
	 */
	Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long currentUserId);

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

	/**
	 * Alcohol의 이미지 URL을 반환하는 메서드입니다.
	 *
	 * @param alcoholId
	 * @return Optional<String>
	 */
	Optional<String> findAlcoholImageUrlById(Long alcoholId);
}
