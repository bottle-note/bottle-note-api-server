package app.bottlenote.picks.service;

import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.fake.FakePicksEventPublisher;
import app.bottlenote.picks.fake.FakePicksRepository;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.fixture.FakeUserFacade;
import app.bottlenote.user.service.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static app.bottlenote.picks.domain.PicksStatus.PICK;
import static app.bottlenote.picks.domain.PicksStatus.UNPICK;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Tag("unit")
@DisplayName("[unit] [service] PicksCommand")
class FakePicksCommandServiceTest {
	private PicksCommandService picksCommandService;

	@BeforeEach
	void setUp() {
		UserFacade userFacade = new FakeUserFacade();
		AlcoholFacade alcoholFacade = new FakeAlcoholFacade();
		PicksRepository picksRepository = new FakePicksRepository();
		HistoryEventPublisher picksEventPublisher = new FakePicksEventPublisher();
		picksCommandService = new PicksCommandService(
			userFacade,
			alcoholFacade,
			picksRepository,
			picksEventPublisher
		);
	}

	@Nested
	@DisplayName("술(위스키)을 찜할 수 있다.")
	class UpdatePicks {

		@Test
		@DisplayName("Picks 된적 없어도 찜할 수 있다.")
		void case_1() {
			// given
			final Long alcoholId = 1L;
			final Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, PICK);

			// when
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

			// then
			assertEquals(PICK, response.getStatus());
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.PICKED.getMessage());
		}

		@Test
		@DisplayName("Picks 된적 있어도 찜할 수 있다.")
		void case_2() {
			// given
			final Long alcoholId = 1L;
			final Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, PICK);

			//when
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

			// then
			assertEquals(PICK, response.getStatus());
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.PICKED.getMessage());
		}

		@Test
		@DisplayName("동일한 상태를 찜해도 찜할 수 있다.")
		void case_3() {
			// given
			final Long alcoholId = 1L;
			final Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, PICK);

			//when
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

			// then
			assertEquals(PICK, response.getStatus());
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
			final Long alcoholId = 1L;
			final Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, UNPICK);

			// when
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
			// then
			assertEquals(UNPICK, response.getStatus());
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.UNPICKED.getMessage());
		}

		@Test
		@DisplayName("UNPICK 된적 있어도 해제할 수 있다.")
		void case_2() {
			// given
			final Long alcoholId = 1L;
			final Long userId = 1L;
			PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, UNPICK);

			//when
			PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

			// then
			assertEquals(UNPICK, response.getStatus());
			assertEquals(response.getMessage(), PicksUpdateResponse.Message.UNPICKED.getMessage());
		}
	}
}
