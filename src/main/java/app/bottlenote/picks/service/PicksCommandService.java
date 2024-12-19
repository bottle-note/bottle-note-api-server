package app.bottlenote.picks.service;

import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.picks.dto.payload.PicksRegistryEvent;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.UserFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;
import static java.lang.Boolean.FALSE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PicksCommandService {

	private final UserFacade userFacade;
	private final AlcoholFacade alcoholFacade;
	private final PicksRepository picksRepository;
	private final HistoryEventPublisher picksEventPublisher;

	/**
	 * 유저가 위스키를 찜/찜해제 상태를 지정하는 로직
	 */
	@Transactional
	public PicksUpdateResponse updatePicks(final PicksUpdateRequest request, final Long userId) {

		Picks picks = picksRepository.findByAlcoholIdAndUserId(request.alcoholId(), userId)
			.orElseGet(() -> {
				if (FALSE.equals(userFacade.existsByUserId(userId))) {
					throw new UserException(USER_NOT_FOUND);
				}
				if (FALSE.equals(alcoholFacade.existsByAlcoholId(request.alcoholId()))) {
					throw new AlcoholException(ALCOHOL_NOT_FOUND);
				}
				return Picks.builder()
					.alcoholId(request.alcoholId())
					.userId(userId)
					.status(request.isPicked())
					.build();
			});

		log.info("pick.getStatus() : {}", picks.getStatus());
		log.info("request.isPicked() : {}", request.isPicked());

		if (picks.getStatus() != request.isPicked()) {
			picksEventPublisher.publishHistoryEvent(
				PicksRegistryEvent.of(picks.getAlcoholId(), picks.getUserId(), request.isPicked()));
		}
		PicksStatus picksStatus = picks.updateStatus(request.isPicked()).getStatus();

		picksRepository.save(picks);
		return PicksUpdateResponse.of(picksStatus);
	}
}
