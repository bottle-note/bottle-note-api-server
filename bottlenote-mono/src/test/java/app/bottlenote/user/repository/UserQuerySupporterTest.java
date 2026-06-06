package app.bottlenote.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import com.querydsl.core.types.Order;
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
}
