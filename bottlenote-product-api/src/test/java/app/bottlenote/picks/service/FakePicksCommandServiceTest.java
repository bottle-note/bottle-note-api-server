package app.bottlenote.picks.service;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.constant.PicksStatus.UNPICK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.alcohols.fixture.FakeAlcoholFacade;
import app.bottlenote.common.event.fixture.FakeApplicationEventPublisher;
import app.bottlenote.picks.domain.PicksRepository;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.event.payload.PicksRegistryEvent;
import app.bottlenote.picks.fake.FakePicksRepository;
import app.bottlenote.user.facade.UserFacade;
import app.bottlenote.user.fixture.FakeUserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] PicksCommand")
class FakePicksCommandServiceTest {
  private PicksCommandService picksCommandService;
  private FakeApplicationEventPublisher picksEventPublisher;

  @BeforeEach
  void setUp() {
    UserFacade userFacade = new FakeUserFacade();
    AlcoholFacade alcoholFacade = new FakeAlcoholFacade();
    PicksRepository picksRepository = new FakePicksRepository();
    picksEventPublisher = new FakeApplicationEventPublisher();
    picksCommandService =
        new PicksCommandService(userFacade, alcoholFacade, picksRepository, picksEventPublisher);
  }

  @Test
  @DisplayName("찜 상태를 반복하고 취소할 때 변경된 상태의 업무 이벤트만 발행한다")
  void publishesTransitionsOnly() {
    picksCommandService.updatePicks(new PicksUpdateRequest(1L, UNPICK), 1L);
    picksCommandService.updatePicks(new PicksUpdateRequest(1L, UNPICK), 1L);
    picksCommandService.updatePicks(new PicksUpdateRequest(1L, PICK), 1L);
    picksCommandService.updatePicks(new PicksUpdateRequest(1L, PICK), 1L);

    assertEquals(
        java.util.List.of(
            new PicksRegistryEvent(1L, 1L, UNPICK), new PicksRegistryEvent(1L, 1L, PICK)),
        picksEventPublisher.getPublishedEventsOfType(PicksRegistryEvent.class));
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
      assertEquals(PICK, response.status());
      assertEquals(response.message(), PicksUpdateResponse.Message.PICKED.message());
    }

    @Test
    @DisplayName("Picks 된적 있어도 찜할 수 있다.")
    void case_2() {
      // given
      final Long alcoholId = 1L;
      final Long userId = 1L;
      PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, PICK);

      // when
      PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

      // then
      assertEquals(PICK, response.status());
      assertEquals(response.message(), PicksUpdateResponse.Message.PICKED.message());
    }

    @Test
    @DisplayName("동일한 상태를 찜해도 찜할 수 있다.")
    void case_3() {
      // given
      final Long alcoholId = 1L;
      final Long userId = 1L;
      PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, PICK);

      // when
      PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

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
      final Long alcoholId = 1L;
      final Long userId = 1L;
      PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, UNPICK);

      // when
      PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);
      // then
      assertEquals(UNPICK, response.status());
      assertEquals(response.message(), PicksUpdateResponse.Message.UNPICKED.message());
    }

    @Test
    @DisplayName("UNPICK 된적 있어도 해제할 수 있다.")
    void case_2() {
      // given
      final Long alcoholId = 1L;
      final Long userId = 1L;
      PicksUpdateRequest pickRequest = new PicksUpdateRequest(alcoholId, UNPICK);

      // when
      PicksUpdateResponse response = picksCommandService.updatePicks(pickRequest, userId);

      // then
      assertEquals(UNPICK, response.status());
      assertEquals(response.message(), PicksUpdateResponse.Message.UNPICKED.message());
    }
  }
}
