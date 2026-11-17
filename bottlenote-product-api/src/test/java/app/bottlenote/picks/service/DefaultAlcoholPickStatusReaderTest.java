package app.bottlenote.picks.service;

import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.constant.PicksStatus.UNPICK;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.fake.FakePicksRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] DefaultAlcoholPickStatusReader")
class DefaultAlcoholPickStatusReaderTest {
  private static final Long ALCOHOL_ID = 100L;
  private static final Long USER_ID = 200L;

  private FakePicksRepository picksRepository;
  private DefaultAlcoholPickStatusReader reader;

  @BeforeEach
  void setUp() {
    picksRepository = new FakePicksRepository();
    reader = new DefaultAlcoholPickStatusReader(picksRepository);
  }

  @Test
  @DisplayName("사용자가 주류를 찜한 상태일 때 true를 반환한다")
  void isPicked_찜한_상태() {
    // given
    save(ALCOHOL_ID, USER_ID, PICK);

    // when & then
    assertThat(reader.isPicked(ALCOHOL_ID, USER_ID)).isTrue();
  }

  @Test
  @DisplayName("사용자가 찜을 취소한 상태일 때 false를 반환한다")
  void isPicked_찜_취소_상태() {
    // given
    save(ALCOHOL_ID, USER_ID, UNPICK);

    // when & then
    assertThat(reader.isPicked(ALCOHOL_ID, USER_ID)).isFalse();
  }

  @Test
  @DisplayName("찜 기록이 없거나 다른 사용자·다른 주류의 찜만 있을 때 false를 반환한다")
  void isPicked_찜_기록_없음() {
    // given
    save(ALCOHOL_ID, USER_ID + 1, PICK);
    save(ALCOHOL_ID + 1, USER_ID, PICK);

    // when & then
    assertThat(reader.isPicked(ALCOHOL_ID, USER_ID)).isFalse();
  }

  @Test
  @DisplayName("같은 사용자와 주류의 찜 기록이 여러 건일 때 찜한 기록이 하나라도 있으면 true를 반환한다")
  void isPicked_중복_기록() {
    // given
    save(ALCOHOL_ID, USER_ID, UNPICK);
    save(ALCOHOL_ID, USER_ID, PICK);

    // when & then
    assertThat(reader.isPicked(ALCOHOL_ID, USER_ID)).isTrue();
  }

  private void save(Long alcoholId, Long userId, PicksStatus status) {
    picksRepository.save(
        Picks.builder().alcoholId(alcoholId).userId(userId).status(status).build());
  }
}
