package app.bottlenote.picks.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.picks.dto.payload.PicksRegistryEvent;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.event.PicksEventPublisher;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class PicksCommandService {

	private final UserQueryRepository userRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;
	private final PicksRepository picksRepository;
	private final PicksEventPublisher picksEventPublisher;

	/**
	 * 유저가 위스키를 찜/찜해제 상태를 지정하는 로직
	 */
	@Transactional
	public PicksUpdateResponse updatePicks(PicksUpdateRequest request, Long userId) {

		Picks picks = picksRepository.findByAlcohol_IdAndUser_Id(request.alcoholId(), userId)
			.orElseGet(() -> {

				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

				Alcohol alcohol = alcoholQueryRepository
					.findById(request.alcoholId())
					.orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

				return Picks.builder()
					.alcohol(alcohol)
					.user(user)
					.status(request.isPicked())
					.build();
			});

		log.info("pick.getStatus() : {}", picks.getStatus());
		log.info("request.isPicked() : {}", request.isPicked());

		if (picks.getStatus() != request.isPicked()) {
			picksEventPublisher.picksRegistry(
				PicksRegistryEvent.of(picks.getAlcohol().getId(), picks.getUser().getId(), request.isPicked()));
		}
		PicksStatus picksStatus = picks.updateStatus(request.isPicked()).getStatus();

		picksRepository.save(picks);
		return PicksUpdateResponse.of(picksStatus);
	}
}
