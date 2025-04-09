package app.bottlenote.picks.service;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.facade.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.constant.PicksStatus.UNPICK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[unit] [service] PicksCommand")
@ExtendWith(MockitoExtension.class)
class PicksCommandServiceTest {

	@InjectMocks
	private PicksCommandService picksCommandService;

	@Mock
	private UserFacade userFacade;

	@Mock
	private AlcoholFacade alcoholFacade;

	@Mock
	private PicksRepository picksRepository;

	@Mock
	private HistoryEventPublisher picksEventPublisher;

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
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PICK);

			// when
			when(userFacade.existsByUserId(anyLong())).thenReturn(Boolean.TRUE);
			when(alcoholFacade.existsByAlcoholId(anyLong())).thenReturn(Boolean.TRUE);

			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
			// then
			assertEquals(PICK, response.status());
			assertEquals(response.message(), PicksUpdateResponse.Message.PICKED.message());
		}

		@Test
		@DisplayName("Picks 된적 있어도 찜할 수 있다.")
		void case_2() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PICK);
			Picks picks = Picks.builder()
					.alcoholId(alcohol.getId())
					.userId(user.getId())
					.status(UNPICK)
					.build();

			//when
			when(picksRepository.findByAlcoholIdAndUserId(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			doNothing().when(picksEventPublisher).publishPicksHistoryEvent(any());
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(PICK, response.status());
			assertEquals(response.message(), PicksUpdateResponse.Message.PICKED.message());
		}

		@Test
		@DisplayName("동일한 상태를 찜해도 찜할 수 있다.")
		void case_3() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), PICK);
			Picks picks = Picks.builder()
					.alcoholId(alcohol.getId())
					.userId(user.getId())
					.status(PICK)
					.build();

			//when
			when(picksRepository.findByAlcoholIdAndUserId(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(PICK, response.status());
			assertEquals(response.message(), PicksUpdateResponse.Message.PICKED.message());
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
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), UNPICK);

			// when
			when(userFacade.existsByUserId(anyLong())).thenReturn(Boolean.TRUE);
			when(alcoholFacade.existsByAlcoholId(anyLong())).thenReturn(Boolean.TRUE);

			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
			// then
			assertEquals(UNPICK, response.status());
			assertEquals(response.message(), PicksUpdateResponse.Message.UNPICKED.message());
		}

		@Test
		@DisplayName("UNPICK 된적 있어도 해제할 수 있다.")
		void case_2() {
			// given
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcohol.getId(), UNPICK);
			Picks picks = Picks.builder()
					.alcoholId(alcohol.getId())
					.userId(user.getId())
					.status(PICK)
					.build();

			//when
			when(picksRepository.findByAlcoholIdAndUserId(alcohol.getId(), user.getId())).thenReturn(Optional.ofNullable(picks));
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, user.getId());

			// then
			assertEquals(UNPICK, response.status());
			assertEquals(response.message(), PicksUpdateResponse.Message.UNPICKED.message());
		}
	}
}
