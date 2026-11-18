package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholPickStatusReader;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultAlcoholFacade 단위 테스트")
class DefaultAlcoholFacadeTest {

  InMemoryAlcoholQueryRepository alcoholRepository;
  RecordingPickStatusReader pickStatusReader;
  DefaultAlcoholFacade alcoholFacade;

  @BeforeEach
  void setUp() {
    alcoholRepository = new SummaryAlcoholQueryRepository();
    pickStatusReader = new RecordingPickStatusReader();
    alcoholFacade = new DefaultAlcoholFacade(alcoholRepository, pickStatusReader);
  }

  // ========== existsByAlcoholId ==========

  @Test
  @DisplayName("술이 존재할 때 true를 반환한다")
  void existsByAlcoholId_술_존재() {
    // given
    Alcohol alcohol = createAlcohol("맥캘란 12년", "Macallan 12");
    alcoholRepository.save(alcohol);

    // when
    Boolean exists = alcoholFacade.existsByAlcoholId(alcohol.getId());

    // then
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("술이 존재하지 않을 때 false를 반환한다")
  void existsByAlcoholId_술_미존재() {
    // given
    Long nonExistentAlcoholId = 999L;

    // when
    Boolean exists = alcoholFacade.existsByAlcoholId(nonExistentAlcoholId);

    // then
    assertThat(exists).isFalse();
  }

  // ========== isValidAlcoholId ==========

  @Test
  @DisplayName("유효하지 않은 술 ID일 때 ALCOHOL_NOT_FOUND 예외가 발생한다")
  void isValidAlcoholId_유효하지_않은_술() {
    // given
    Long invalidAlcoholId = 999L;

    // when & then
    assertThatThrownBy(() -> alcoholFacade.isValidAlcoholId(invalidAlcoholId))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
  }

  // ========== findAlcoholImageUrlById ==========

  @Test
  @DisplayName("술 이미지 URL을 정확히 조회할 수 있다")
  void findAlcoholImageUrlById_정상_조회() {
    // given
    Alcohol alcohol = createAlcohol("글렌피딕 15년", "Glenfiddich 15");
    alcoholRepository.save(alcohol);

    // when
    Optional<String> imageUrl = alcoholFacade.findAlcoholImageUrlById(alcohol.getId());

    // then
    assertThat(imageUrl).isPresent();
    assertThat(imageUrl.get()).isEqualTo("https://example.com/glenfiddich.jpg");
  }

  @Test
  @DisplayName("존재하지 않는 술의 이미지 조회 시 예외가 발생한다")
  void findAlcoholImageUrlById_존재하지_않는_술() {
    // given
    Long nonExistentAlcoholId = 999L;

    // when & then
    assertThatThrownBy(() -> alcoholFacade.findAlcoholImageUrlById(nonExistentAlcoholId))
        .isInstanceOf(AlcoholException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
  }

  // ========== getAlcoholSummaryItemWithNext ==========

  @Test
  @DisplayName("존재하지 않는 술 조회 시 null Pair를 반환한다")
  void getAlcoholSummaryItemWithNext_존재하지_않는_술() {
    // given
    Long nonExistentAlcoholId = 999L;

    // when
    var result = alcoholFacade.getAlcoholSummaryItemWithNext(nonExistentAlcoholId);

    // then
    assertThat(result.getLeft()).isNull();
    assertThat(result.getRight()).isNull();
  }

  // ========== findAlcoholInfoById ==========

  @Test
  @DisplayName("술 정보를 조회할 때 빈 Optional을 반환한다")
  void findAlcoholInfoById_빈_Optional() {
    // given
    Long alcoholId = 999L;

    // when
    var result = alcoholFacade.findAlcoholInfoById(alcoholId, null);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("찜한 로그인 사용자가 술 정보를 조회할 때 isPicked를 true로 채운다")
  void findAlcoholInfoById_찜한_사용자() {
    // given
    Alcohol alcohol = alcoholRepository.save(createAlcohol("맥캘란 12년", "Macallan 12"));
    Long userId = 7L;
    pickStatusReader.pick(alcohol.getId(), userId);

    // when
    Optional<AlcoholSummaryItem> result =
        alcoholFacade.findAlcoholInfoById(alcohol.getId(), userId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().alcoholId()).isEqualTo(alcohol.getId());
    assertThat(result.get().korName()).isEqualTo("맥캘란 12년");
    assertThat(result.get().isPicked()).isTrue();
  }

  @Test
  @DisplayName("찜하지 않은 로그인 사용자가 술 정보를 조회할 때 isPicked를 false로 채운다")
  void findAlcoholInfoById_찜하지_않은_사용자() {
    // given
    Alcohol alcohol = alcoholRepository.save(createAlcohol("맥캘란 12년", "Macallan 12"));
    pickStatusReader.pick(alcohol.getId(), 7L);

    // when
    Optional<AlcoholSummaryItem> result = alcoholFacade.findAlcoholInfoById(alcohol.getId(), 8L);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().isPicked()).isFalse();
    assertThat(pickStatusReader.calls()).isEqualTo(1);
  }

  @Test
  @DisplayName("비로그인(null, -1)으로 술 정보를 조회할 때 찜을 조회하지 않고 isPicked를 false로 채운다")
  void findAlcoholInfoById_비로그인() {
    // given
    Alcohol alcohol = alcoholRepository.save(createAlcohol("맥캘란 12년", "Macallan 12"));
    pickStatusReader.pick(alcohol.getId(), -1L);

    // when
    Optional<AlcoholSummaryItem> asNull = alcoholFacade.findAlcoholInfoById(alcohol.getId(), null);
    Optional<AlcoholSummaryItem> asGuest = alcoholFacade.findAlcoholInfoById(alcohol.getId(), -1L);

    // then
    assertThat(asNull).isPresent();
    assertThat(asNull.get().isPicked()).isFalse();
    assertThat(asGuest).isPresent();
    assertThat(asGuest.get().isPicked()).isFalse();
    assertThat(pickStatusReader.calls()).isZero();
  }

  @Test
  @DisplayName("삭제된 술의 정보를 조회할 때 빈 Optional을 반환하고 찜을 조회하지 않는다")
  void findAlcoholInfoById_삭제된_술() {
    // given
    Alcohol alcohol = alcoholRepository.save(createAlcohol("맥캘란 12년", "Macallan 12"));
    pickStatusReader.pick(alcohol.getId(), 7L);
    alcohol.delete();

    // when
    Optional<AlcoholSummaryItem> result = alcoholFacade.findAlcoholInfoById(alcohol.getId(), 7L);

    // then
    assertThat(result).isEmpty();
    assertThat(pickStatusReader.calls()).isZero();
  }

  // ========== Helper Methods ==========

  private Alcohol createAlcohol(String korName, String engName) {
    return Alcohol.builder()
        .korName(korName)
        .engName(engName)
        .type(AlcoholType.WHISKY)
        .korCategory("위스키")
        .engCategory("Whisky")
        .categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
        .imageUrl("https://example.com/glenfiddich.jpg")
        .build();
  }

  /** 실제 조회처럼 삭제되지 않은 술의 요약을 isPicked=false로 돌려주는 대역 */
  private static final class SummaryAlcoholQueryRepository extends InMemoryAlcoholQueryRepository {
    @Override
    public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {
      return findById(alcoholId)
          .filter(alcohol -> !alcohol.isDeleted())
          .map(
              alcohol ->
                  new AlcoholSummaryItem(
                      alcohol.getId(),
                      alcohol.getKorName(),
                      alcohol.getEngName(),
                      alcohol.getKorCategory(),
                      alcohol.getEngCategory(),
                      alcohol.getImageUrl(),
                      false));
    }
  }

  /** 찜 조회 호출 횟수를 기록하는 대역 */
  private static final class RecordingPickStatusReader implements AlcoholPickStatusReader {
    private final Set<String> picked = new HashSet<>();
    private int calls;

    void pick(Long alcoholId, Long userId) {
      picked.add(alcoholId + ":" + userId);
    }

    int calls() {
      return calls;
    }

    @Override
    public boolean isPicked(Long alcoholId, Long userId) {
      calls++;
      return picked.contains(alcoholId + ":" + userId);
    }
  }
}
