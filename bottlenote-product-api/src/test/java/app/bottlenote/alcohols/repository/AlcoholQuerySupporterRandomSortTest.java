package app.bottlenote.alcohols.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.querydsl.core.types.OrderSpecifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [repository] AlcoholQuerySupporter - Random Sorting")
class AlcoholQuerySupporterRandomSortTest {

  private final AlcoholQuerySupporter supporter = new AlcoholQuerySupporter();

  @Test
  @DisplayName("최적화된 랜덤 정렬 조건을 생성할 수 있다")
  void test_1() {
    // given
    Long cursor = 0L;

    // when
    OrderSpecifier<?> orderSpecifier = supporter.sortByOptimizedRandom(cursor);

    // then
    assertNotNull(orderSpecifier, "정렬 조건이 생성되어야 합니다");
    assertTrue(orderSpecifier.isAscending(), "오름차순 정렬이어야 합니다");
  }

  @Test
  @DisplayName("다른 cursor 값으로 다른 정렬 조건을 생성한다")
  void test_2() {
    // given
    Long cursor1 = 0L;
    Long cursor2 = 20L;

    // when
    OrderSpecifier<?> order1 = supporter.sortByOptimizedRandom(cursor1);
    OrderSpecifier<?> order2 = supporter.sortByOptimizedRandom(cursor2);

    // then
    assertNotNull(order1);
    assertNotNull(order2);
    // 다른 cursor는 다른 정렬 기준을 생성해야 함
    // SQL 표현식이 다를 것으로 기대
    assertTrue(!order1.toString().equals(order2.toString()), "다른 cursor는 다른 정렬 조건을 생성해야 합니다");
  }

  @Test
  @DisplayName("null cursor는 기본값으로 처리된다")
  void test_3() {
    // given
    Long cursor = null;

    // when
    OrderSpecifier<?> orderSpecifier = supporter.sortByOptimizedRandom(cursor);

    // then
    assertNotNull(orderSpecifier, "null cursor도 정렬 조건을 생성해야 합니다");
  }

  @Test
  @DisplayName("같은 cursor는 같은 정렬 조건을 생성한다")
  void test_4() {
    // given
    Long cursor = 10L;

    // when
    OrderSpecifier<?> order1 = supporter.sortByOptimizedRandom(cursor);
    OrderSpecifier<?> order2 = supporter.sortByOptimizedRandom(cursor);

    // then
    assertNotNull(order1);
    assertNotNull(order2);
    // 같은 cursor는 같은 정렬 조건을 생성해야 함
    assertTrue(order1.toString().equals(order2.toString()), "같은 cursor는 같은 정렬 조건을 생성해야 합니다");
  }

  @Test
  @DisplayName("기존 랜덤 정렬 메서드도 여전히 동작한다")
  void test_5() {
    // when
    OrderSpecifier<?> orderSpecifier = supporter.sortByRandom();

    // then
    assertNotNull(orderSpecifier, "기존 랜덤 정렬도 동작해야 합니다");
    assertTrue(orderSpecifier.isAscending(), "오름차순 정렬이어야 합니다");
  }
}
