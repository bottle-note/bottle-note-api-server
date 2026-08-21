package app.bottlenote.mfds.dto.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("MFDS 검색 criteria 단위 테스트")
class MfdsSearchCriteriaTest {

  @Test
  @DisplayName("신고 criteria를 생성할 때 커서·페이지 크기 기본값을 적용한다")
  void 신고_criteria_기본값을_적용할_수_있다() {
    MfdsDeclarationSearchCriteria criteria =
        new MfdsDeclarationSearchCriteria(null, null, null, null, null, null, null);

    assertThat(criteria.cursor()).isZero();
    assertThat(criteria.pageSize()).isEqualTo(MfdsDeclarationSearchCriteria.DEFAULT_SIZE);
    assertThat(criteria.hasCursor()).isFalse();
    assertThat(criteria.fetchLimit()).isEqualTo(MfdsDeclarationSearchCriteria.DEFAULT_SIZE + 1);
  }

  @Test
  @DisplayName("신고 criteria의 페이지 크기가 상한을 넘을 때 최대값으로 제한한다")
  void 신고_criteria_페이지_크기를_제한할_수_있다() {
    MfdsDeclarationSearchCriteria criteria =
        new MfdsDeclarationSearchCriteria(null, null, null, null, null, 10L, 500L);

    assertThat(criteria.pageSize()).isEqualTo(MfdsDeclarationSearchCriteria.MAX_SIZE);
    assertThat(criteria.hasCursor()).isTrue();
  }

  @Test
  @DisplayName("신고 criteria의 공백 키워드와 매칭 결정을 null로 정규화한다")
  void 신고_criteria_공백_입력을_정규화할_수_있다() {
    MfdsDeclarationSearchCriteria criteria =
        new MfdsDeclarationSearchCriteria(null, null, "  ", null, " 글렌피딕 ", 0L, 20L);

    assertThat(criteria.alcoholMatchDecision()).isNull();
    assertThat(criteria.keyword()).isEqualTo("글렌피딕");
  }

  @Test
  @DisplayName("수입사 criteria를 생성할 때 커서·페이지 크기 기본값을 적용한다")
  void 수입사_criteria_기본값을_적용할_수_있다() {
    MfdsImporterSearchCriteria criteria = new MfdsImporterSearchCriteria(null, null, null, null);

    assertThat(criteria.cursor()).isZero();
    assertThat(criteria.pageSize()).isEqualTo(MfdsImporterSearchCriteria.DEFAULT_SIZE);
    assertThat(criteria.hasCursor()).isFalse();
    assertThat(criteria.fetchLimit()).isEqualTo(MfdsImporterSearchCriteria.DEFAULT_SIZE + 1);
  }

  @Test
  @DisplayName("수입사 criteria의 음수 커서와 공백 키워드를 정규화한다")
  void 수입사_criteria_입력을_정규화할_수_있다() {
    MfdsImporterSearchCriteria criteria = new MfdsImporterSearchCriteria(null, "  ", -5L, 0L);

    assertThat(criteria.cursor()).isZero();
    assertThat(criteria.keyword()).isNull();
    assertThat(criteria.pageSize()).isEqualTo(MfdsImporterSearchCriteria.DEFAULT_SIZE);
  }
}
