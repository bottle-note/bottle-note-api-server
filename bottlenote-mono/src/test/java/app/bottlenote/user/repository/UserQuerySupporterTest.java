package app.bottlenote.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import com.querydsl.core.types.Order;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] UserQuerySupporter")
class UserQuerySupporterTest {

  private final UserQuerySupporter supporter = new UserQuerySupporter(null);

  @DisplayName("탭에 join되지 않은 평점 정렬은 사용자와 술 기준 서브쿼리로 생성한다")
  @Test
  void sortBy_whenRatingSortRequestedFromPickTab_usesCorrelatedSubquery() {
    var orderSpecifier =
        supporter.sortBy(MyBottleType.PICK, MyBottleSortType.RATING, SortOrder.DESC, 1L);

    assertThat(orderSpecifier.getOrder()).isEqualTo(Order.DESC);
    assertThat(orderSpecifier.getTarget().toString())
        .contains("select")
        .contains("rating")
        .contains("alcohol.id")
        .contains("rating.id.userId = ?1");
  }

  @DisplayName("탭에 join되지 않은 리뷰 정렬은 사용자와 술 기준 서브쿼리로 생성한다")
  @Test
  void sortBy_whenReviewSortRequestedFromRatingTab_usesCorrelatedSubquery() {
    var orderSpecifier =
        supporter.sortBy(MyBottleType.RATING, MyBottleSortType.REVIEW, SortOrder.ASC, 1L);

    assertThat(orderSpecifier.getOrder()).isEqualTo(Order.ASC);
    assertThat(orderSpecifier.getTarget().toString())
        .contains("select")
        .contains("review")
        .contains("alcohol.id")
        .contains("review.userId = ?1");
  }

  @DisplayName("마이보틀 조회 결과가 pageSize보다 많으면 응답 목록에서는 lookahead 항목을 제거한다")
  @Test
  void myBottlePageItems_whenHasLookahead_removesLookaheadFromResponseItems() {
    var request = new MyBottlePageableCriteria(1L, null, null, null, null, 0L, 2L, 1L);
    var items = List.of("first", "second", "lookahead");

    var pageItems = supporter.myBottlePageItems(request, items);

    assertThat(pageItems).containsExactly("first", "second");
  }

  @DisplayName("마이보틀 조회 결과가 pageSize 이하면 응답 목록을 그대로 유지한다")
  @Test
  void myBottlePageItems_whenNoLookahead_keepsResponseItems() {
    var request = new MyBottlePageableCriteria(1L, null, null, null, null, 0L, 3L, 1L);
    var items = List.of("first", "second");

    var pageItems = supporter.myBottlePageItems(request, items);

    assertThat(pageItems).containsExactly("first", "second");
  }

  @DisplayName("마이보틀 보조 정렬은 요청 정렬 방향과 같은 alcohol id 정렬을 생성한다")
  @Test
  void myBottleTieBreakerSortBy_usesAlcoholIdWithRequestedOrder() {
    var orderSpecifier = supporter.myBottleTieBreakerSortBy(SortOrder.ASC);

    assertThat(orderSpecifier.getOrder()).isEqualTo(Order.ASC);
    assertThat(orderSpecifier.getTarget().toString()).isEqualTo("alcohol.id");
  }
}