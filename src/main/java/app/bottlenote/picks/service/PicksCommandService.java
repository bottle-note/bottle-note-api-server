package app.bottlenote.picks.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PicksCommandService {

	private final UserRepository userRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;
	private final PicksRepository picksRepository;

	/**
	 * 유저가 위스키를 찜/찜해제 상태를 지정하는 로직
	 */
	public Object updatePicks(PicksUpdateRequest request, Long userId) {

		Picks picks = picksRepository.findByAlcohol_IdAndUser_Id(request.alcoholId(), userId)
			.orElseGet(() -> {
				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
				Alcohol alcohol = alcoholQueryRepository
					.findById(request.alcoholId())
					.orElseThrow(() -> new IllegalArgumentException("해당 술이 존재하지 않습니다."));
				return Picks.builder()
					.alcohol(alcohol)
					.user(user)
					.status(request.isPicked())
					.build();
			});

		PicksStatus picksStatus = picks.updateStatus(request.isPicked()).getStatus();
		return PicksUpdateResponse.of(picksStatus);
	}
}
