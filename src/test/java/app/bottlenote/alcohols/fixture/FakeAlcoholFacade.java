package app.bottlenote.alcohols.fixture;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeAlcoholFacade implements AlcoholFacade {

	private static final Logger log = LogManager.getLogger(FakeAlcoholFacade.class);
	
	private final Map<Long, AlcoholInfo> alcoholDatabase = new ConcurrentHashMap<>();

	public FakeAlcoholFacade() {
		alcoholDatabase.put(1L, new AlcoholInfo(
			1L,
			"위스키",
			"Whiskey",
			"위스키 카테고리",
			"Whiskey Category",
			"https://bottlenote.app/alcohol/1",
			false
		));
		alcoholDatabase.put(2L, new AlcoholInfo(
			2L,
			"럼",
			"Rum",
			"럼 카테고리",
			"Rum Category",
			"https://bottlenote.app/alcohol/2",
			false
		));
		alcoholDatabase.put(3L, new AlcoholInfo(
			3L,
			"보드카",
			"Vodka",
			"보드카 카테고리",
			"Vodka Category",
			"https://bottlenote.app/alcohol/3",
			false
		));
	}

	/**
	 * Utility method to add alcohol data for testing purposes.
	 *
	 * @param alcoholInfo Alcohol information to add.
	 */
	public void addAlcohol(AlcoholInfo alcoholInfo) {
		Objects.requireNonNull(alcoholInfo, "AlcoholInfo cannot be null");
		alcoholDatabase.put(alcoholInfo.alcoholId(), alcoholInfo);
		log.debug("Added alcohol: {}", alcoholInfo);
	}

	/**
	 * Utility method to remove alcohol data by ID.
	 *
	 * @param alcoholId ID of the alcohol to remove.
	 */
	public void removeAlcoholById(Long alcoholId) {
		alcoholDatabase.remove(alcoholId);
		log.debug("Removed alcohol with ID: {}", alcoholId);
	}

	/**
	 * Utility method to clear all alcohol data.
	 */
	public void clearAlcoholDatabase() {
		alcoholDatabase.clear();
		log.debug("Cleared all alcohol data");
	}

	@Override
	public Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long currentUserId) {
		AlcoholInfo alcoholInfo = alcoholDatabase.get(alcoholId);
		if (alcoholInfo != null) {
			log.debug("Found AlcoholInfo for ID {}: {}", alcoholId, alcoholInfo);
			return Optional.of(alcoholInfo);
		} else {
			log.debug("No AlcoholInfo found for ID {}", alcoholId);
			return Optional.empty();
		}
	}

	@Override
	public Boolean existsByAlcoholId(Long alcoholId) {
		boolean exists = alcoholDatabase.containsKey(alcoholId);
		log.debug("Exists check for Alcohol ID {}: {}", alcoholId, exists);
		return exists;
	}

	@Override
	public void isValidAlcoholId(Long alcoholId) {
		if (!existsByAlcoholId(alcoholId)) {
			log.error("Alcohol ID {} not found", alcoholId);
			throw new AlcoholException(ALCOHOL_NOT_FOUND);
		}
	}

	@Override
	public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
		AlcoholInfo alcoholInfo = alcoholDatabase.get(alcoholId);
		if (alcoholInfo != null) {
			log.debug("Found image URL for Alcohol ID {}: {}", alcoholId, alcoholInfo.imageUrl());
			return Optional.of(alcoholInfo.imageUrl());
		} else {
			log.debug("No image URL found for Alcohol ID {}", alcoholId);
			return Optional.empty();
		}
	}
}