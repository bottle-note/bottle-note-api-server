package app.bottlenote.global.util;

import app.bottlenote.shared.cursor.SortOrder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;

// todo : infrastructure 모듈로 이동 필요
public class SortOrderUtils {
  public static <T extends Comparable<?>> OrderSpecifier<T> resolve(
      SortOrder sortOrder, ComparableExpressionBase<T> expression) {
    return sortOrder == SortOrder.DESC ? expression.desc() : expression.asc();
  }
}
