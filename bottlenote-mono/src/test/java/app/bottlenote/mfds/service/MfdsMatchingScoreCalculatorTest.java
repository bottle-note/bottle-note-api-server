package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetail;
import app.bottlenote.mfds.fixture.MfdsTestData;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MfdsMatchingScoreCalculator 단위 테스트")
class MfdsMatchingScoreCalculatorTest {

  private final MfdsMatchingScoreCalculator calculator = new MfdsMatchingScoreCalculator();

  @Test
  @DisplayName("이름이 완전히 일치할 때 이름 점수와 총점이 1이다")
  void 이름_완전_일치시_만점을_준다() {
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 12", "Glenfiddich 12");

    MfdsMatchScoreDetail detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.totalScore()).isEqualByComparingTo(BigDecimal.ONE);
  }

  @Test
  @DisplayName("이름이 부분적으로 일치할 때 0과 1 사이 점수를 준다")
  void 이름_부분_일치시_중간_점수를_준다() {
    MfdsDeclaration declaration = declaration(null, "glenfiddich 12 special reserve");
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 12", "Glenfiddich 12");

    MfdsMatchScoreDetail detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isGreaterThan(BigDecimal.ZERO);
    assertThat(detail.nameScore()).isLessThan(BigDecimal.ONE);
  }

  @Test
  @DisplayName("이름 정보가 전혀 없을 때 총점은 0이다")
  void 이름_정보가_없으면_총점_0이다() {
    MfdsDeclaration declaration = declaration(null, null);
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 12", "Glenfiddich 12");

    MfdsMatchScoreDetail detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isNull();
    assertThat(detail.totalScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("도수가 일치하는 후보가 불일치 후보보다 총점이 높다")
  void 도수_불일치시_감점한다() {
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "abvPercent", new BigDecimal("40.0"));

    AlcoholMatchTargetItem abvMatched =
        new AlcoholMatchTargetItem(
            1L,
            "글렌피딕 12",
            "Glenfiddich 12",
            "40.0",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    AlcoholMatchTargetItem abvMismatched =
        new AlcoholMatchTargetItem(
            2L,
            "글렌피딕 12",
            "Glenfiddich 12",
            "55.0",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    MfdsMatchScoreDetail matchedDetail = calculator.scoreAlcohol(declaration, abvMatched);
    MfdsMatchScoreDetail mismatchedDetail = calculator.scoreAlcohol(declaration, abvMismatched);

    assertThat(matchedDetail.abvScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(mismatchedDetail.abvScore()).isLessThan(matchedDetail.abvScore());
    assertThat(mismatchedDetail.totalScore()).isLessThan(matchedDetail.totalScore());
  }

  @Test
  @DisplayName("숙성 연수가 일치하면 1점, 1년 차이는 0.5점, 그 이상은 0점을 준다")
  void 숙성_연수_근접도를_점수화한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "ageYears", (short) 12);

    MfdsMatchScoreDetail exact = calculator.scoreAlcohol(declaration, alcoholWithAge(1L, "12"));
    MfdsMatchScoreDetail nearby = calculator.scoreAlcohol(declaration, alcoholWithAge(2L, "13"));
    MfdsMatchScoreDetail far = calculator.scoreAlcohol(declaration, alcoholWithAge(3L, "18"));

    assertThat(exact.ageScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(nearby.ageScore()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(far.ageScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("카테고리가 일치할 때 카테고리 점수가 1이다")
  void 카테고리_일치를_점수화한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "alcoholCategoryEn", "Single Malt");

    AlcoholMatchTargetItem target =
        new AlcoholMatchTargetItem(
            1L,
            "글렌피딕",
            "Glenfiddich",
            null,
            null,
            "싱글 몰트",
            "Single Malt",
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    MfdsMatchScoreDetail detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.categoryScore()).isEqualByComparingTo(BigDecimal.ONE);
  }

  @Test
  @DisplayName("증류소 이름 후보가 없을 때 증류소 점수는 null이다")
  void 증류소_이름_후보가_없으면_점수가_없다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");

    BigDecimal score =
        calculator.scoreDistillery(
            declaration, new DistilleryMatchTargetItem(1L, "글렌피딕 증류소", "Glenfiddich Distillery"));

    assertThat(score).isNull();
  }

  @Test
  @DisplayName("증류소 이름 후보가 일치할 때 1점을 준다")
  void 증류소_이름_일치를_점수화한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "distilleryNameEnCandidate", "Glenfiddich");

    BigDecimal score =
        calculator.scoreDistillery(
            declaration, new DistilleryMatchTargetItem(1L, "글렌피딕", "Glenfiddich"));

    assertThat(score).isEqualByComparingTo(BigDecimal.ONE);
  }

  @Test
  @DisplayName("제조국 이름으로 지역 후보를 점수화할 수 있다")
  void 제조국_이름으로_지역을_점수화한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "manufactureCountryNameEn", "United Kingdom");

    BigDecimal matched =
        calculator.scoreRegion(declaration, new RegionMatchTargetItem(1L, "영국", "United Kingdom"));
    BigDecimal unrelated =
        calculator.scoreRegion(declaration, new RegionMatchTargetItem(2L, "일본", "Japan"));

    assertThat(matched).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(unrelated).isLessThan(matched);
  }

  private MfdsDeclaration declaration(String nameKo, String nameEn) {
    return MfdsTestData.declaration(
        "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, nameKo, nameEn);
  }

  private AlcoholMatchTargetItem alcohol(Long id, String korName, String engName) {
    return new AlcoholMatchTargetItem(
        id, korName, engName, null, null, null, null, null, null, null, null, null, null, null);
  }

  private AlcoholMatchTargetItem alcoholWithAge(Long id, String age) {
    return new AlcoholMatchTargetItem(
        id, "글렌피딕", "Glenfiddich", null, age, null, null, null, null, null, null, null, null, null);
  }
}
