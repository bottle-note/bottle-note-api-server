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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("MfdsMatchingScoreCalculator 단위 테스트")
class MfdsMatchingScoreCalculatorTest {

  private final MfdsMatchingScoreCalculator calculator = new MfdsMatchingScoreCalculator();

  @Test
  @DisplayName("원문에 CS가 있을 때 정제 검색 키에서 빠져도 CS 후보를 우선한다")
  void 원문_CS를_보존한다() {
    var source = declaration("일리악 CS", "ILEACH CASK STRENGTH");
    MfdsTestData.set(source, "nameSearchKeyEn", "ileach");
    var cs = calculator.scoreAlcohol(source, alcohol(1L, "일리악 CS", "Ileach Cask Strength"));
    var regular = calculator.scoreAlcohol(source, alcohol(2L, "일리악", "Ileach"));
    assertThat(cs.totalScore()).isGreaterThanOrEqualTo(new BigDecimal("0.4"));
    assertThat(cs.totalScore()).isGreaterThan(regular.totalScore());
    assertThat(status(cs, "CASK_STRENGTH")).isEqualTo("MATCH");
    assertThat(status(regular, "CASK_STRENGTH")).isEqualTo("UNKNOWN");
    assertThat(regular.reviewRequired()).isTrue();
  }

  @ParameterizedTest
  @CsvSource({
    "Glenmorangie 12yo, Glenmorangie The Original 12yo, Glenmorangie X",
    "The Hakushu Single Malt Japanese Whisky Distillers Reserve, Hakushu Distillers Reserve, Akkeshi Single Malt Japanese Whisky Risshun",
    "Glencadam Aged 15 Years Highland Single Malt Scotch Whisky, Glencadam 15y, Westward Single Malt Whiskey",
    "Laphroaig 10yo Cask Strength Batch 17, Laphroaig 10yo CS Batch 17, Glenallachie 10yo CS Batch 17",
    "Signatory Craigellachie 16Y, Signatory 2007 Craigellachie 16yo 100 Proof Edition, Signatory 2009 Glenallachie 12yo CS",
    "Dalmore 12yo Sherry Cask, Dalmore 12yo Sherry Cask Select, Armorik Sherry Cask",
    "Port Charlotte PMC 01, Port Charlotte PMC 01, Port Charlotte Islay Barley 2013",
    "Jim Beam Kentucky Straight Bourbon Whiskey, Jim Beam, Town Branch Kentucky Straight Bourbon Whiskey",
    "Balvenie 12yo Double Wood, Balvenie 12yo Doublewood, Balvenie 17yo Doublewood"
  })
  @DisplayName("실제 실패 유형을 비교할 때 동일 제품 후보가 다른 제품보다 앞선다")
  void 제품_식별_정보를_우선한다(String sourceName, String expected, String other) {
    var source = declaration(null, sourceName);
    var match = calculator.scoreAlcohol(source, alcohol(1L, null, expected));
    var mismatch = calculator.scoreAlcohol(source, alcohol(2L, null, other));
    assertThat(match.totalScore()).isGreaterThanOrEqualTo(new BigDecimal("0.4"));
    assertThat(match.totalScore()).isGreaterThan(mismatch.totalScore());
  }

  @ParameterizedTest
  @CsvSource({
    "Pappy Van Winkle 15yo, Pappy Van Winkle 23yo, AGE",
    "Laphroaig 10yo CS Batch 17, Laphroaig 10yo CS Batch 16, BATCH",
    "Glenlivet Cask 123, Glenlivet Cask 124, CASK",
    "Shenks 2024, Shenks 2023, YEAR",
    "Ballantines 23yo Golden Hour Edition 2, Ballantines 23yo Golden Hour Edition 1, EDITION",
    "Glenmorangie Original 12yo, Glenmorangie Original 13yo, AGE"
  })
  @DisplayName("양쪽 식별 속성이 다를 때 후보 기준보다 낮게 감점한다")
  void 명확한_불일치를_제외한다(String sourceName, String other, String attribute) {
    var result = calculator.scoreAlcohol(declaration(null, sourceName), alcohol(1L, null, other));
    assertThat(result.totalScore()).isLessThan(new BigDecimal("0.4"));
    assertThat(status(result, attribute)).isEqualTo("MISMATCH");
  }

  @Test
  @DisplayName("브랜드와 숙성만 있을 때 여러 제품을 확인 필요 후보로 남긴다")
  void 제품명이_부족하면_확인_필요로_남긴다() {
    var source = declaration(null, "Glenmorangie 12yo");
    for (String name : new String[] {"Glenmorangie Original 12yo", "Glenmorangie Lasanta 12yo"}) {
      var result = calculator.scoreAlcohol(source, alcohol(1L, null, name));
      assertThat(result.totalScore()).isGreaterThanOrEqualTo(new BigDecimal("0.4"));
      assertThat(result.reviewRequired()).isTrue();
      assertThat(status(result, "PRODUCT")).isEqualTo("UNKNOWN");
    }
  }

  @Test
  @DisplayName("배치가 한쪽에 없을 때 불일치와 구별하고 확인 필요로 남긴다")
  void 배치_누락과_불일치를_구별한다() {
    var source = declaration(null, "Laphroaig 10yo CS Batch 17");
    var unknown = calculator.scoreAlcohol(source, alcohol(1L, null, "Laphroaig 10yo CS"));
    var mismatch = calculator.scoreAlcohol(source, alcohol(2L, null, "Laphroaig 10yo CS Batch 16"));
    assertThat(status(unknown, "BATCH")).isEqualTo("UNKNOWN");
    assertThat(unknown.reviewRequired()).isTrue();
    assertThat(unknown.totalScore()).isGreaterThan(mismatch.totalScore());
  }

  @Test
  @DisplayName("브랜드가 다를 때 일반 단어가 같아도 후보에서 제외한다")
  void 공통_단어만으로_브랜드를_넘지_않는다() {
    var result =
        calculator.scoreAlcohol(
            declaration(null, "Glen Moray 12yo Single Malt Scotch Whisky"),
            alcohol(1L, null, "Glen Grant 12yo Single Malt Scotch Whisky"));
    assertThat(result.totalScore()).isLessThan(new BigDecimal("0.4"));
    assertThat(status(result, "BRAND")).isEqualTo("MISMATCH");
  }

  @Test
  @DisplayName("16Y가 원문에 있을 때 숙성 필드가 없어도 추출한다")
  void 원문에서_숙성을_복구한다() {
    var result =
        calculator.scoreAlcohol(
            declaration(null, "Signatory Craigellachie 16Y"),
            alcohol(1L, null, "Signatory Craigellachie 16yo"));
    assertThat(result.ageScore()).isEqualByComparingTo(BigDecimal.ONE);
  }

  @Test
  @DisplayName("원문과 숙성 필드가 충돌할 때 원문을 비교하고 확인 필요로 남긴다")
  void 입력_충돌을_표시한다() {
    var source = declaration(null, "Glencadam 15yo");
    MfdsTestData.set(source, "ageYears", (short) 5);
    var result = calculator.scoreAlcohol(source, alcohol(1L, null, "Glencadam 15yo"));
    assertThat(result.reviewRequired()).isTrue();
    assertThat(status(result, "SOURCE_AGE")).isEqualTo("MISMATCH");
  }

  @Test
  @DisplayName("신고가 브랜디일 때 같은 이름의 위스키를 추천하지 않는다")
  void 다른_주종을_제외한다() {
    var source = declaration(null, "Hennessy Paradis");
    MfdsTestData.set(source, "alcoholCategoryEn", "Brandy");
    var target =
        new AlcoholMatchTargetItem(
            1L,
            null,
            "Hennessy Paradis",
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
    var result = calculator.scoreAlcohol(source, target);
    assertThat(result.totalScore()).isLessThan(new BigDecimal("0.4"));
    assertThat(status(result, "CATEGORY")).isEqualTo("MISMATCH");
  }

  @Test
  @DisplayName("이름 정보가 없을 때 도수와 숙성이 같아도 추천하지 않는다")
  void 이름이_없으면_점수는_0이다() {
    var result =
        calculator.scoreAlcohol(declaration(null, null), alcohol(1L, "글렌피딕", "Glenfiddich"));
    assertThat(result.totalScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @ParameterizedTest
  @CsvSource({"40, 1", "45, 0.5", "50, 0", "55, 0"})
  @DisplayName("도수를 비교할 때 차이에 따른 점수는 0과 1 사이로 제한한다")
  void 도수_경계값을_검증한다(String abv, String expected) {
    var source = declaration(null, "Glenfiddich 12yo");
    MfdsTestData.set(source, "abvPercent", new BigDecimal("40"));
    var target =
        new AlcoholMatchTargetItem(
            1L,
            null,
            "Glenfiddich 12yo",
            abv,
            "12",
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
    assertThat(calculator.scoreAlcohol(source, target).abvScore()).isEqualByComparingTo(expected);
  }

  @Test
  @DisplayName("대상 브랜드가 없을 때 숙성과 도수만 같아도 후보로 올리지 않는다")
  void 브랜드_없는_대상을_추천하지_않는다() {
    var result =
        calculator.scoreAlcohol(
            declaration(null, "Pappy Van Winkle 15yo"), alcohol(1L, null, "15yo"));
    assertThat(result.totalScore()).isLessThan(new BigDecimal("0.4"));
    assertThat(result.reviewRequired()).isTrue();
  }

  @Test
  @DisplayName("공개 이름이 이전 확정값으로 바뀌어도 신고 SKU로 다시 계산한다")
  void 확정된_이름보다_원본_SKU를_우선한다() {
    var source = declaration(null, "Laphroaig 10yo CS Batch 16");
    MfdsTestData.set(source, "alcoholNameEn", "Laphroaig 10yo CS Batch 16");
    MfdsTestData.set(source, "skuDisplayNameEn", "Laphroaig 10yo CS Batch 17 700ml");
    var expected = calculator.scoreAlcohol(source, alcohol(1L, null, "Laphroaig 10yo CS Batch 17"));
    var previous = calculator.scoreAlcohol(source, alcohol(2L, null, "Laphroaig 10yo CS Batch 16"));
    assertThat(expected.totalScore()).isGreaterThanOrEqualTo(new BigDecimal("0.4"));
    assertThat(previous.totalScore()).isLessThan(new BigDecimal("0.4"));
  }

  private String status(MfdsMatchScoreDetailItem detail, String attribute) {
    return detail.comparisons().stream()
        .filter(c -> c.attribute().equals(attribute))
        .findFirst()
        .orElseThrow()
        .status();
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
        id, korName, engName, null, null, null, null, null, null, null, null, null, null, null,
        null);
  }
}
