package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssueItem;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("알코올 벌크 수치 정규화")
class AlcoholBulkInputNormalizerTest {
  @ParameterizedTest
  @CsvSource({
    "abv,40,40%",
    "abv, 40.50 % ,40.5%",
    "abv,0,0%",
    "abv,100%,100%",
    "volume,70 cl,700ml",
    "volume,0.7 L,700ml",
    "volume,700,700ml",
    "volume,'1,000 ml',1000ml",
    "volume,'1,000.5 cl',10005ml"
  })
  @DisplayName("숫자와 단위가 유효할 때 표준 단위로 정규화한다")
  void 수치를_정규화한다(String field, String input, String expected) {
    var errors = new ArrayList<AdminAlcoholBulkIssueItem>();
    var warnings = new ArrayList<AdminAlcoholBulkIssueItem>();
    assertThat(AlcoholBulkInputNormalizer.quantity(input, field, errors, warnings))
        .isEqualTo(expected);
    assertThat(errors).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "abv,-1",
    "abv,100.01%",
    "abv,40ml",
    "abv,NaN",
    "abv,Infinity",
    "abv,forty",
    "abv,'4,0%'",
    "abv,40-101%",
    "abv,50-40%",
    "abv,40/200%",
    "abv,batch 1: invalid",
    "volume,0",
    "volume,-700ml",
    "volume,700%",
    "volume,700oz",
    "volume,'70,0ml'",
    "volume,'1,00,000ml'",
    "volume,700mL garbage",
    "volume,700ml x 0",
    "volume,0ml x 2",
    "volume,700ml x -2",
    "volume,700ml/garbage",
    "volume,1e3",
    "volume,700ml/",
    "abv,40%%",
    "abv,약101(배치마다상이)%",
    "abv,58.5(쓰레기)%",
    "volume,1L(700ml)",
    "volume,'1,,000ml'"
  })
  @DisplayName("값이나 단위가 잘못됐을 때 오류를 반환한다")
  void 잘못된_수치를_거절한다(String field, String input) {
    var errors = new ArrayList<AdminAlcoholBulkIssueItem>();
    var warnings = new ArrayList<AdminAlcoholBulkIssueItem>();
    assertThat(AlcoholBulkInputNormalizer.quantity(input, field, errors, warnings)).isNull();
    assertThat(errors)
        .extracting(AdminAlcoholBulkIssueItem::code)
        .containsExactly("INVALID_QUANTITY");
    assertThat(warnings).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "abv,40-46%", "abv,40% / 43%", "abv,batch 1: 40%", "abv,'Batch 1: 40%, Batch 2: 43%'",
    "abv,약60(배치마다상이)%", "abv,63.4(제품마다상이)%", "abv,58.5(캐스크 스트렝스)%", "volume,1L(1000ml)",
    "volume,700ml x 2", "volume,2 x 700ml", "volume,700ml + 50ml", "volume,0.7-1L"
  })
  @DisplayName("범위와 배치 및 세트 표현이 유효할 때 원문과 경고를 반환한다")
  void 복합_표현을_보존한다(String field, String input) {
    var errors = new ArrayList<AdminAlcoholBulkIssueItem>();
    var warnings = new ArrayList<AdminAlcoholBulkIssueItem>();
    assertThat(AlcoholBulkInputNormalizer.quantity(input, field, errors, warnings))
        .isEqualTo(input);
    assertThat(errors).isEmpty();
    assertThat(warnings)
        .extracting(AdminAlcoholBulkIssueItem::code)
        .containsExactly("NON_SCALAR_VALUE");
  }
}
