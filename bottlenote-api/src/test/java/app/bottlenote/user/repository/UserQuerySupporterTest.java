package app.bottlenote.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import com.querydsl.core.types.Order;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] UserQuerySupporter")
class UserQuerySupporterTest {

  private final UserQuerySupporter supporter = new UserQuerySupporter(null);
  private final HmacCursorCodec cursorCodec = newCursorCodec();

  private static HmacCursorCodec newCursorCodec() {
    CursorProperties properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("test-secret-key-at-least-32-bytes-long");
    return new HmacCursorCodec(properties, Clock.systemUTC());
  }

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
    var request = new MyBottlePageableCriteria(1L, null, null, null, null, null, 2, 1L, Set.of());
    var items = List.of("first", "second", "lookahead");

    var pageItems = supporter.myBottlePageItems(request, items);

    assertThat(pageItems).containsExactly("first", "second");
  }

  @DisplayName("마이보틀 조회 결과가 pageSize 이하면 응답 목록을 그대로 유지한다")
  @Test
  void myBottlePageItems_whenNoLookahead_keepsResponseItems() {
    var request = new MyBottlePageableCriteria(1L, null, null, null, null, null, 3, 1L, Set.of());
    var items = List.of("first", "second");

    var pageItems = supporter.myBottlePageItems(request, items);

    assertThat(pageItems).containsExactly("first", "second");
  }

  @DisplayName("마이보틀 보조 정렬은 PICK/RATING 탭에서 요청 정렬 방향과 같은 alcohol id 정렬을 생성한다")
  @Test
  void myBottleTieBreakerSortBy_whenTabIsPick_usesAlcoholIdWithRequestedOrder() {
    var orderSpecifier = supporter.myBottleTieBreakerSortBy(SortOrder.ASC, MyBottleType.PICK);

    assertThat(orderSpecifier.getOrder()).isEqualTo(Order.ASC);
    assertThat(orderSpecifier.getTarget().toString()).isEqualTo("alcohol.id");
  }

  @DisplayName("REVIEW 탭의 마이보틀 보조 정렬은 alcohol id가 아니라 review id를 사용한다")
  @Test
  void myBottleTieBreakerSortBy_whenTabIsReview_usesReviewId() {
    var orderSpecifier = supporter.myBottleTieBreakerSortBy(SortOrder.DESC, MyBottleType.REVIEW);

    assertThat(orderSpecifier.getOrder()).isEqualTo(Order.DESC);
    assertThat(orderSpecifier.getTarget().toString()).isEqualTo("review.id");
  }

  @DisplayName("정렬 값이 null인 항목을 커서로 인코딩하면 해당 정렬 키가 커서에 담기지 않는다")
  @Test
  void encodeMyBottleCursor_whenSortValueIsNull_omitsSortKeyFromCursor() {
    var request =
        new MyBottlePageableCriteria(
            1L, null, null, MyBottleSortType.REVIEW, SortOrder.DESC, null, 10, 1L, Set.of());

    String cursor =
        supporter.encodeMyBottleCursor(
            MyBottleType.PICK, request, 5L, null, null, null, cursorCodec);
    CursorClaims claims = cursorCodec.verify(cursor, request.context(MyBottleType.PICK));

    assertThat(claims.sortKeys()).containsEntry("id", "5").doesNotContainKey("t");
  }

  @DisplayName("커서의 정렬 값이 null이면 NULL 꼬리 구간에 진입한 것으로 보고 타이브레이커만으로 이어가는 seek을 생성한다")
  @Test
  void myBottleSeek_whenCursorSortValueIsNull_buildsNullTailSeek() {
    var request =
        new MyBottlePageableCriteria(
            1L, null, null, MyBottleSortType.LATEST, SortOrder.DESC, null, 10, 1L, Set.of());
    String cursor =
        supporter.encodeMyBottleCursor(
            MyBottleType.PICK, request, 5L, null, null, null, cursorCodec);
    var requestWithCursor =
        new MyBottlePageableCriteria(
            1L, null, null, MyBottleSortType.LATEST, SortOrder.DESC, cursor, 10, 1L, Set.of());

    var seek = supporter.myBottleSeek(MyBottleType.PICK, requestWithCursor, cursorCodec);

    String expression = seek.toString();
    assertThat(expression).containsIgnoringCase("is null");
    assertThat(expression).doesNotContain("||");
  }
}
