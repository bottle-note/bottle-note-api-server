package app.bottlenote.picks.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.repository.UserCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PicksCommandServiceTest {

	@InjectMocks
	private PicksCommandService picksCommandService;

	@Mock
	private UserCommandRepository userRepository;

	@Mock
	private AlcoholQueryRepository alcoholQueryRepository;

	@Mock
	private PicksRepository picksRepository;

	private User user;
	private Alcohol alcohol;

	@BeforeEach
	void setUp() {
		user = User.builder().id(1L).nickName("유저").build();
		alcohol = Alcohol.builder().id(1L).korName("유저").build();
	}

	@Nested
	@DisplayName("술(위스키)을 찜할 수 있다.")
	class UpdatePicks {

		@Test
		@DisplayName("Picks 된적 없어도 찜할 수 있다.")
		void case_1() {
			// given
			Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PicksStatus.PICK);

			// when
			when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(user));
			when(alcoholQueryRepository.findById(alcohol.getId())).thenReturn(Optional.ofNullable(alcohol));

			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
			// then
			assertEquals(response.getStatus(), PicksStatus.PICK);
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.PICKED.getMessage());
		}

		@Test
		@DisplayName("Picks 된적 있어도 찜할 수 있다.")
		void case_2() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PicksStatus.PICK);
			Picks picks = Picks.builder()
				.alcohol(alcohol)
				.user(user)
				.status(PicksStatus.UNPICK)
				.build();

			//when
			when(picksRepository.findByAlcohol_IdAndUser_Id(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(response.getStatus(), PicksStatus.PICK);
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.PICKED.getMessage());
		}

		@Test
		@DisplayName("동일한 상태를 찜해도 찜할 수 있다.")
		void case_3() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PicksStatus.PICK);
			Picks picks = Picks.builder()
				.alcohol(alcohol)
				.user(user)
				.status(PicksStatus.PICK)
				.build();

			//when
			when(picksRepository.findByAlcohol_IdAndUser_Id(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(response.getStatus(), PicksStatus.PICK);
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.PICKED.getMessage());
		}
	}

	@Nested
	@DisplayName("술(위스키)을 찜 해제하는 할 수 있다.")
	class UpdateUnPicks {

		@Test
		@DisplayName("UNPICK 된적 없어도 찜 해제할 수 있다.")
		void case_1() {
			// given
			Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PicksStatus.UNPICK);

			// when
			when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(user));
			when(alcoholQueryRepository.findById(alcohol.getId())).thenReturn(Optional.ofNullable(alcohol));

			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
			// then
			assertEquals(response.getStatus(), PicksStatus.UNPICK);
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.UNPICKED.getMessage());
		}

		@Test
		@DisplayName("UNPICK 된적 있어도 해제할 수 있다.")
		void case_2() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PicksStatus.UNPICK);
			Picks picks = Picks.builder()
				.alcohol(alcohol)
				.user(user)
				.status(PicksStatus.PICK)
				.build();

			//when
			when(picksRepository.findByAlcohol_IdAndUser_Id(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(response.getStatus(), PicksStatus.UNPICK);
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.UNPICKED.getMessage());
		}
	}
}
