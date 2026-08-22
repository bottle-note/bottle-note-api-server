package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AlcoholQuerySupporter 단위 테스트")
class AlcoholQuerySupporterTest {

  private final AlcoholQuerySupporter supporter = new AlcoholQuerySupporter(null);

  @Test
  @DisplayName("POPULAR 점수는 snapshot 점수를 double로 변환하고 누락 점수는 0으로 처리한다")
  void popularScore_whenSnapshotMissing_coalescesToZeroDouble() {
    // when
    NumberExpression<Double> score = supporter.popularScore();

    // then
    assertThat(score.toString())
        .contains("popularAlcohol.popularScore")
        .contains("coalesce")
        .contains("0.0");
  }

  @Test
  @DisplayName("POPULAR sortScore와 orderBy target은 같은 snapshot 점수 표현식을 사용한다")
  void popularSort_whenOrdered_reusesSnapshotScoreExpression() {
    // given
    NumberExpression<Double> score = supporter.popularScore();

    // when
    NumberExpression<? extends Number> sortScore = supporter.sortScore(SearchSortType.POPULAR);
    OrderSpecifier<?> orderSpecifier = supporter.sortBy(SearchSortType.POPULAR, SortOrder.DESC);
    Expression<?> orderTarget = orderSpecifier.getTarget();

    // then
    assertThat(sortScore.toString()).isEqualTo(score.toString());
    assertThat(orderTarget.toString()).isEqualTo(score.toString());
    assertThat(orderSpecifier.isAscending()).isFalse();
  }

  @Test
  @DisplayName("POPULAR snapshot 조인은 주류별 최신 createdAt 행만 연결한다")
  void latestPopularSnapshot_joinsOnlyLatestCreatedAtPerAlcohol() {
    // when
    BooleanExpression condition = supporter.latestPopularSnapshot();

    // then
    assertThat(condition.toString())
        .contains("popularAlcohol.alcoholId = alcohol.id")
        .contains("latestPopularAlcohol")
        .contains("max")
        .contains("alcoholId = alcohol.id");
  }
}
