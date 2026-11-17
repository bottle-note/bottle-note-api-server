package app.bottlenote.rating.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.NumberExpression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("unit")
@DisplayName("RatingQuerySupporter 단위 테스트")
class RatingQuerySupporterTest {

  // eqAlcoholRegion 등 미사용 경로에서만 참조되는 협력자라 테스트 대상 메서드에는 영향이 없다
  private final RatingQuerySupporter supporter = new RatingQuerySupporter(null);

  @ParameterizedTest(name = "{0} 정렬일 때 orderBy와 sortScore가 같은 표현식을 반환한다")
  @EnumSource(
      value = SearchSortType.class,
      names = {"POPULAR", "RATING", "PICK", "REVIEW"})
  @DisplayName("RANDOM이 아닌 정렬 타입일 때 orderBy와 sortScore가 동일한 정렬 표현식을 반환한다")
  void orderBy_whenNotRandom_matchesSortScoreExpression(SearchSortType sortType) {
    // given
    OrderSpecifier<?> orderSpecifier = supporter.orderBy(sortType, SortOrder.DESC);
    NumberExpression<? extends Number> sortScore = supporter.sortScore(sortType);

    // when
    Expression<?> orderTarget = orderSpecifier.getTarget();

    // then
    // seek(having/where)과 ORDER BY가 같은 표현식이어야 keyset 페이지네이션이 어긋나지 않는다
    assertThat(orderTarget.toString()).isEqualTo(sortScore.toString());
  }

  @ParameterizedTest(name = "정렬 방향 {0}에 따라 orderBy가 올바른 방향을 갖는다")
  @EnumSource(SortOrder.class)
  @DisplayName("정렬 방향에 따라 POPULAR orderBy의 방향이 결정된다")
  void orderBy_whenSortOrderGiven_reflectsDirection(SortOrder sortOrder) {
    // given & when
    OrderSpecifier<?> orderSpecifier = supporter.orderBy(SearchSortType.POPULAR, sortOrder);

    // then
    boolean expectedAscending = sortOrder == SortOrder.ASC;
    assertThat(orderSpecifier.isAscending()).isEqualTo(expectedAscending);
  }

  @Test
  @DisplayName("RANDOM 정렬은 sortScore를 지원하지 않아 예외를 던진다")
  void sortScore_whenRandom_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> supporter.sortScore(SearchSortType.RANDOM))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("crc32Rank는 seed가 다르면 다른 CRC32 표현식을 생성한다")
  void crc32Rank_whenSeedDiffers_generatesDifferentExpression() {
    // given
    NumberExpression<Long> crcWithSeed1 = supporter.crc32Rank(1L);
    NumberExpression<Long> crcWithSeed2 = supporter.crc32Rank(2L);

    // when & then
    assertThat(crcWithSeed1.toString()).isNotEqualTo(crcWithSeed2.toString());
  }

  @Test
  @DisplayName("crc32Rank는 같은 seed면 같은 CRC32 표현식을 생성한다")
  void crc32Rank_whenSeedSame_generatesSameExpression() {
    // given
    NumberExpression<Long> first = supporter.crc32Rank(42L);
    NumberExpression<Long> second = supporter.crc32Rank(42L);

    // when & then
    assertThat(first.toString()).isEqualTo(second.toString());
  }
}
