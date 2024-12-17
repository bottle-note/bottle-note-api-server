package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class FakeAlcoholFacade implements AlcoholFacade {

	private static final Logger log = LogManager.getLogger(FakeAlcoholFacade.class);

	@Override
	public Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long currentUserId) {
		return Optional.empty();
	}

	@Override
	public Boolean existsByAlcoholId(Long alcoholId) {
		return true;
	}

	@Override
	public void isValidAlcoholId(Long alcoholId) {

	}

	@Override
	public Optional<String> findAlcoholImageUrlById(Long alcoholId) {
		return Optional.of("https://bottlenote.app/alcohol/1");
	}
}
