package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QPopularAlcohol.popularAlcohol;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.JoinExpression;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
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
    assertThat(condition).isInstanceOf(Operation.class);
    Operation<?> rootAnd = (Operation<?>) condition;
    assertThat(rootAnd.getOperator()).isEqualTo(Ops.AND);

    Operation<?> joinsSnapshotToAlcohol = (Operation<?>) rootAnd.getArg(0);
    assertThat(joinsSnapshotToAlcohol.getOperator()).isEqualTo(Ops.EQ);
    assertThat(joinsSnapshotToAlcohol.getArgs()).containsExactly(popularAlcohol.alcoholId, alcohol.id);

    Operation<?> matchesLatestCreatedAt = (Operation<?>) rootAnd.getArg(1);
    assertThat(matchesLatestCreatedAt.getOperator()).isEqualTo(Ops.EQ);
    assertThat(matchesLatestCreatedAt.getArg(0)).isEqualTo(popularAlcohol.createdAt);
    assertThat(matchesLatestCreatedAt.getArg(1)).isInstanceOf(SubQueryExpression.class);

    SubQueryExpression<?> latestCreatedAt =
        (SubQueryExpression<?>) matchesLatestCreatedAt.getArg(1);
    QueryMetadata metadata = latestCreatedAt.getMetadata();
    assertThat(metadata.getJoins()).hasSize(1);
    JoinExpression join = metadata.getJoins().get(0);
    assertThat(join.getTarget()).isInstanceOf(Path.class);
    assertThat(((Path<?>) join.getTarget()).getMetadata().getName())
        .isEqualTo("latestPopularAlcohol");

    assertThat(metadata.getProjection()).isInstanceOf(Operation.class);
    Operation<?> maxCreatedAt = (Operation<?>) metadata.getProjection();
    assertThat(maxCreatedAt.getOperator()).isEqualTo(Ops.AggOps.MAX_AGG);
    assertThat(maxCreatedAt.getArgs()).hasSize(1);
    assertThat(maxCreatedAt.getArg(0)).isInstanceOf(Path.class);
    Path<?> createdAt = (Path<?>) maxCreatedAt.getArg(0);
    assertThat(createdAt.getMetadata().getName()).isEqualTo("createdAt");
    assertThat(createdAt.getRoot().getMetadata().getName()).isEqualTo("latestPopularAlcohol");

    assertThat(metadata.getWhere()).isInstanceOf(Operation.class);
    Operation<?> correlation = (Operation<?>) metadata.getWhere();
    assertThat(correlation.getOperator()).isEqualTo(Ops.EQ);
    assertThat(correlation.getArgs()).hasSize(2);
    assertThat(correlation.getArg(0)).isInstanceOf(Path.class);
    Path<?> latestAlcoholId = (Path<?>) correlation.getArg(0);
    assertThat(latestAlcoholId.getMetadata().getName()).isEqualTo("alcoholId");
    assertThat(latestAlcoholId.getRoot().getMetadata().getName()).isEqualTo("latestPopularAlcohol");
    assertThat(correlation.getArg(1)).isEqualTo(alcohol.id);
  }
}
