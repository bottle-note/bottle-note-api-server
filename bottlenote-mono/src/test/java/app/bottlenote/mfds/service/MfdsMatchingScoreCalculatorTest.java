package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.response.MfdsMatchScoreDetailItem;
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

    MfdsMatchScoreDetailItem detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.totalScore()).isEqualByComparingTo(BigDecimal.ONE);
  }

  @Test
  @DisplayName("이름이 부분적으로 일치할 때 0과 1 사이 점수를 준다")
  void 이름_부분_일치시_중간_점수를_준다() {
    MfdsDeclaration declaration = declaration(null, "glenfiddich 12 special reserve");
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 12", "Glenfiddich 12");

    MfdsMatchScoreDetailItem detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isGreaterThan(BigDecimal.ZERO);
    assertThat(detail.nameScore()).isLessThan(BigDecimal.ONE);
  }

  @Test
  @DisplayName("이름 정보가 전혀 없을 때 총점은 0이다")
  void 이름_정보가_없으면_총점_0이다() {
    MfdsDeclaration declaration = declaration(null, null);
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 12", "Glenfiddich 12");

    MfdsMatchScoreDetailItem detail = calculator.scoreAlcohol(declaration, target);

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
            null,
            null);

    MfdsMatchScoreDetailItem matchedDetail = calculator.scoreAlcohol(declaration, abvMatched);
    MfdsMatchScoreDetailItem mismatchedDetail = calculator.scoreAlcohol(declaration, abvMismatched);

    assertThat(matchedDetail.abvScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(mismatchedDetail.abvScore()).isLessThan(matchedDetail.abvScore());
    assertThat(mismatchedDetail.totalScore()).isLessThan(matchedDetail.totalScore());
  }

  @Test
  @DisplayName("숙성 연수가 일치하면 1점, 1년 차이는 0.5점, 그 이상은 0점을 준다")
  void 숙성_연수_근접도를_점수화한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "ageYears", (short) 12);

    MfdsMatchScoreDetailItem exact = calculator.scoreAlcohol(declaration, alcoholWithAge(1L, "12"));
    MfdsMatchScoreDetailItem nearby =
        calculator.scoreAlcohol(declaration, alcoholWithAge(2L, "13"));
    MfdsMatchScoreDetailItem far = calculator.scoreAlcohol(declaration, alcoholWithAge(3L, "18"));

    assertThat(exact.ageScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(nearby.ageScore()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(far.ageScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("신고에 숙성 연수가 있으면 대상 age가 없어도 빈티지 점수를 주지 않는다")
  void 숙성_연수가_있으면_빈티지_분기를_타지_않는다() {
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "ageYears", (short) 12);
    MfdsTestData.set(declaration, "vintageYear", (short) 2012);

    MfdsMatchScoreDetailItem detail =
        calculator.scoreAlcohol(declaration, alcohol(1L, "글렌피딕 2012", "Glenfiddich 2012"));

    assertThat(detail.ageScore()).isNull();
    // 판단 불가는 0점이 아니라 가중치 제외다. 총점이 이름 점수와 같아야 분모에서 빠진 것이다
    assertThat(detail.totalScore()).isEqualByComparingTo(detail.nameScore());
  }

  @Test
  @DisplayName("신고에 숙성 연수가 없고 빈티지 연도만 있으면 이름에 연도가 있을 때 1점을 준다")
  void 숙성_연수가_없을_때_빈티지_연도로_대체한다() {
    MfdsDeclaration declaration = declaration("글렌피딕", "glenfiddich");
    MfdsTestData.set(declaration, "vintageYear", (short) 2012);

    MfdsMatchScoreDetailItem withYear =
        calculator.scoreAlcohol(declaration, alcohol(1L, "글렌피딕 2012", "Glenfiddich 2012"));
    MfdsMatchScoreDetailItem withoutYear =
        calculator.scoreAlcohol(declaration, alcohol(2L, "글렌피딕", "Glenfiddich"));

    assertThat(withYear.ageScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(withoutYear.ageScore()).isNull();
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
            null,
            null);

    MfdsMatchScoreDetailItem detail = calculator.scoreAlcohol(declaration, target);

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

  @Test
  @DisplayName("이름만 비교 가능할 때 총점은 이름 점수와 정확히 같다")
  void 이름만_비교_가능하면_총점은_이름_점수와_같다() {
    // 재정규화 분모가 0.5가 아니면(예: 1.0 고정) 총점이 이름 점수의 절반이 되어 깨진다
    MfdsDeclaration declaration = declaration("글렌피딕 12", null);
    AlcoholMatchTargetItem target = alcohol(1L, "글렌피딕 15", null);

    MfdsMatchScoreDetailItem detail = calculator.scoreAlcohol(declaration, target);

    assertThat(detail.nameScore()).isEqualByComparingTo(new BigDecimal("0.857143"));
    assertThat(detail.totalScore()).isEqualByComparingTo(new BigDecimal("0.857143"));
  }

  @Test
  @DisplayName("이름과 도수만 비교 가능할 때 총점은 가중합을 0.65로 나눈 값이다")
  void 이름과_도수만_비교_가능하면_남은_가중치로_재정규화한다() {
    // (0.5 * 1.0 + 0.15 * 0.5) / (0.5 + 0.15) = 0.884615. 분모를 1.0으로 두면 0.575가 된다
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "abvPercent", new BigDecimal("40.0"));

    MfdsMatchScoreDetailItem detail =
        calculator.scoreAlcohol(declaration, alcoholWithAbv(1L, "45.0"));

    assertThat(detail.nameScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(detail.abvScore()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(detail.totalScore()).isEqualByComparingTo(new BigDecimal("0.884615"));
  }

  @Test
  @DisplayName("도수 차이가 0이면 1점, 5면 0.5점, 10이면 0점이다")
  void 도수_근접도의_경계값을_점수화한다() {
    // 허용 오차 10 기준의 선형 감점이다. 경계 세 지점을 기대값으로 고정한다
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "abvPercent", new BigDecimal("40.0"));

    MfdsMatchScoreDetailItem same =
        calculator.scoreAlcohol(declaration, alcoholWithAbv(1L, "40.0"));
    MfdsMatchScoreDetailItem half =
        calculator.scoreAlcohol(declaration, alcoholWithAbv(2L, "45.0"));
    MfdsMatchScoreDetailItem zero =
        calculator.scoreAlcohol(declaration, alcoholWithAbv(3L, "50.0"));

    assertThat(same.abvScore()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(half.abvScore()).isEqualByComparingTo(new BigDecimal("0.5"));
    assertThat(zero.abvScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("도수 차이가 허용 오차를 넘어도 음수가 아니라 0점으로 고정한다")
  void 도수_차이가_허용_오차를_넘으면_0으로_고정한다() {
    // 클램프가 없으면 -0.5가 되어 총점을 끌어내린다
    MfdsDeclaration declaration = declaration("글렌피딕 12", "glenfiddich 12");
    MfdsTestData.set(declaration, "abvPercent", new BigDecimal("40.0"));

    MfdsMatchScoreDetailItem detail =
        calculator.scoreAlcohol(declaration, alcoholWithAbv(1L, "55.0"));

    assertThat(detail.abvScore()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(detail.totalScore()).isEqualByComparingTo(new BigDecimal("0.769231"));
  }

  @Test
  @DisplayName("대상 도수가 없을 때 도수 요소를 가중치에서 제외한다")
  void 대상_도수가_없으면_도수_요소를_제외한다() {
    // 0점 처리와 구분해야 한다. 제외라면 총점이 이름 점수 그대로 남는다
    MfdsDeclaration declaration = declaration("글렌피딕 12", null);
    MfdsTestData.set(declaration, "abvPercent", new BigDecimal("40.0"));

    MfdsMatchScoreDetailItem detail =
        calculator.scoreAlcohol(declaration, alcohol(1L, "글렌피딕 15", null));

    assertThat(detail.abvScore()).isNull();
    assertThat(detail.totalScore()).isEqualByComparingTo(detail.nameScore());
    assertThat(detail.totalScore()).isEqualByComparingTo(new BigDecimal("0.857143"));
  }

  private MfdsDeclaration declaration(String nameKo, String nameEn) {
    return MfdsTestData.declaration(
        "RCNO-001", MfdsNormalizationStatus.NORMALIZED, null, null, null, nameKo, nameEn);
  }

  private AlcoholMatchTargetItem alcohol(Long id, String korName, String engName) {
    return new AlcoholMatchTargetItem(
        id, korName, engName, null, null, null, null, null, null, null, null, null, null, null,
        null);
  }

  private AlcoholMatchTargetItem alcoholWithAbv(Long id, String abv) {
    return new AlcoholMatchTargetItem(
        id,
        "글렌피딕 12",
        "Glenfiddich 12",
        abv,
        null,
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
  }

  private AlcoholMatchTargetItem alcoholWithAge(Long id, String age) {
    return new AlcoholMatchTargetItem(
        id,
        "글렌피딕",
        "Glenfiddich",
        null,
        age,
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
  }
}
