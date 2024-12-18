package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

public class FakeAlcoholFacade implements AlcoholFacade {

	private List<Long> alcoholDatabase = List.of(1L, 2L, 3L);

	private static final Logger log = LogManager.getLogger(FakeAlcoholFacade.class);

	@Override
	public Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long currentUserId) {
		return Optional.empty();
	}

	@Override
	public Boolean existsByAlcoholId(Long alcoholId) {
		return alcoholDatabase.contains(alcoholId);
	}

	@Override
	public void isValidAlcoholId(Long alcoholId) {
		if (!existsByAlcoholId(alcoholId)) {
			throw new AlcoholException(ALCOHOL_NOT_FOUND);
		}
	}

	@Override
	public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
		return Optional.of("https://bottlenote.app/alcohol/" + alcoholId);
	}
}
