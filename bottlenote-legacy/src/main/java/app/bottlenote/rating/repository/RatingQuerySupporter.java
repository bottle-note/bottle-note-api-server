package app.bottlenote.rating.repository;

import static app.bottlenote.core.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.shared.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.SortOrder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RatingQuerySupporter {

  /**
   * CursorPageable 생성
   *
   * @param resultList the resultList
   * @param cursor the cursor
   * @param pageSize the page size
   * @return the cursor pageable
   */
  protected CursorPageable getCursorPageable(List<?> resultList, Long pageSize, Long cursor) {
    Objects.requireNonNull(resultList, "조회 결과 목록은 필수입니다.");
    Objects.requireNonNull(cursor, "커서는 필수입니다.");
    Objects.requireNonNull(pageSize, "조회 할 페이지 크기는 필수입니다.");

    int resultSize = resultList.size();
    boolean hasNext = resultSize > pageSize; // 다음 페이지가 있는지 확인
    if (hasNext) {
      resultList.remove(resultSize - 1);
    }

    return CursorPageable.builder()
        .currentCursor(cursor)
        .cursor(cursor + pageSize) // 다음 페이지가 있는 경우 마지막으로 가져온 ID를 다음 커서로 사용
        .pageSize(pageSize)
        .hasNext(hasNext)
        .build();
  }

  /**
   * 마이 페이지 사용자의 평점 개수를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @return 평점 개수
   */
  public Expression<Long> ratingCountSubQuery(Long userId) {
    return ExpressionUtils.as(
        select(rating.count())
            .from(rating)
            .where(rating.id.userId.eq(userId).and(rating.ratingPoint.rating.gt(0.0))),
        "ratingCount");
  }

  public Expression<Double> averageRatingSubQuery(NumberPath<Long> alocholId) {
    return ExpressionUtils.as(
        select(rating.ratingPoint.rating.avg().round())
            .from(rating)
            .where(rating.id.alcoholId.eq(alocholId).and(rating.ratingPoint.rating.gt(0.0))),
        "averageRatingPoint");
  }

  public Expression<Long> averageRatingCountSubQuery(NumberPath<Long> alocholId) {
    return ExpressionUtils.as(
        select(rating.ratingPoint.rating.count())
            .from(rating)
            .where(rating.id.alcoholId.eq(alocholId).and(rating.ratingPoint.rating.gt(0.0))),
        "averageRatingCount");
  }

  /** 술 이름을 검색하는 조건 */
  protected BooleanExpression eqAlcoholName(String name) {

    if (StringUtils.isNullOrEmpty(name)) return null;

    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 카테고리를 검색하는 조건 */
  protected BooleanExpression eqAlcoholCategory(AlcoholCategoryGroup category) {

    if (Objects.isNull(category)) return null;

    return alcohol.categoryGroup.stringValue().like("%" + category + "%");
  }

  /** 리전을 검색하는 조건 */
  protected BooleanExpression eqAlcoholRegion(Long regionId) {
    if (regionId == null) return null;

    return alcohol.region.id.eq(regionId);
  }

  /**
   * 내가 별점을 안준 술만 조회
   *
   * @param userId the user id
   * @return the boolean expression
   */
  public BooleanExpression neRatingByMe(Long userId) {
    if (userId == null) return null;

    return rating.id.userId.isNull().or(rating.id.userId.ne(userId));
  }

  /** 1차 정렬 조건 - RANDOM - POPULAR - RATING - PICK - REVIEW */
  protected OrderSpecifier<?> orderBy(SearchSortType searchSortType, SortOrder sortOrder) {
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg(); // 평균 평점 계산
    NumberExpression<Long> reviewCount = review.id.countDistinct(); // 고유 리뷰 수 계산
    NumberExpression<Long> pickCount = picks.id.countDistinct(); // 고유 좋아요 수 계산

    return switch (searchSortType) {
      case POPULAR ->
          sortOrder == SortOrder.DESC
              ? avgRating.add(reviewCount).desc()
              : avgRating.add(reviewCount).asc();
      case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
      case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
      case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
      case RANDOM -> Expressions.numberTemplate(Double.class, "function('rand')").asc();
    };
  }

  /** 2차 정렬 조건 (랜덤) */
  protected OrderSpecifier<?> orderByRandom() {
    return Expressions.numberTemplate(Double.class, "function('rand')").asc();
  }
}
