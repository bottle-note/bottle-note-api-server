package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("DefaultAlcoholFacade 단위 테스트")
class DefaultAlcoholFacadeTest {

  InMemoryAlcoholQueryRepository alcoholRepository;
  DefaultAlcoholFacade alcoholFacade;

  @BeforeEach
  void setUp() {
    alcoholRepository = new InMemoryAlcoholQueryRepository();
    alcoholFacade = new DefaultAlcoholFacade(alcoholRepository);
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
        .hasFieldOrPropertyWithValue("code", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
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
        .hasFieldOrPropertyWithValue("code", AlcoholExceptionCode.ALCOHOL_NOT_FOUND);
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

  // ========== Helper Methods ==========

  private Alcohol createAlcohol(String korName, String engName) {
    return Alcohol.builder()
        .korName(korName)
        .engName(engName)
        .type(AlcoholType.WHISKY)
        .korCategory("위스키")
        .engCategory("Whisky")
        .imageUrl("https://example.com/glenfiddich.jpg")
        .build();
  }
}
