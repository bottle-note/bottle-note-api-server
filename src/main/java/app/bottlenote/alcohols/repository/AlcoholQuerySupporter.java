package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.alcohols.domain.QPopularAlcohol.popularAlcohol;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.alcohols.domain.QTastingTag.tastingTag;
import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.KeywordTagMapping;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlcoholQuerySupporter {

  /** 주류에 연결된 테이스팅 태그 목록을 문자열로 조회 */
  public static Expression<String> getTastingTags() {
    return ExpressionUtils.as(
        JPAExpressions.select(Expressions.stringTemplate("group_concat({0})", tastingTag.korName))
            .from(alcoholsTastingTags)
            .join(tastingTag)
            .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
            .where(alcoholsTastingTags.alcohol.id.eq(alcohol.id)),
        "alcoholsTastingTags");
  }

  /** 주어진 주류가 인기 순위에 포함되는지 확인 */
  public BooleanExpression isHot5(NumberPath<Long> id) {
    LocalDateTime now = LocalDateTime.now();
    return select(popularAlcohol.count())
        .from(popularAlcohol)
        .where(
            popularAlcohol.alcoholId.eq(id),
            popularAlcohol.year.eq(now.getYear()),
            popularAlcohol.month.eq(now.getMonthValue()),
            popularAlcohol.day.eq(now.getDayOfMonth()))
        .gt(0L);
  }

  /** 사용자가 주류에 찜하기를 했는지 확인하는 서브쿼리 생성 */
  public BooleanExpression pickedSubQuery(Long userId) {
    if (userId == -1) return Expressions.asBoolean(false);

    return JPAExpressions.selectOne()
        .from(picks)
        .where(picks.alcoholId.eq(alcohol.id), picks.userId.eq(userId), picks.status.eq(PICK))
        .exists();
  }

  /** 특정 주류가 사용자에 의해 찜되었는지 확인 */
  public BooleanExpression isPickedSubquery(Long alcoholId, Long userId) {
    if (userId == -1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(picks)
                .where(
                    picks
                        .alcoholId
                        .eq(alcoholId)
                        .and(picks.userId.eq(userId))
                        .and(picks.status.eq(PICK)))
                .exists())
        .as("isPicked");
  }

  /** 사용자가 주류에 준 평점 조회 */
  public NumberExpression<Double> myRating(Long alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(rating.ratingPoint.rating)
                .from(rating)
                .where(rating.id.alcoholId.eq(alcoholId).and(rating.id.userId.eq(userId)))
                .limit(1))
        .coalesce(0.0)
        .castToNum(Double.class)
        .as("myRating");
  }

  /** 사용자가 주류에 작성한 리뷰들의 평균 평점 계산 */
  public NumberExpression<Double> averageReviewRating(Long alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(
                    review
                        .reviewRating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0))
                .from(review)
                .where(review.alcoholId.eq(alcoholId).and(review.userId.eq(userId))))
        .castToNum(Double.class)
        .as("myAvgRating");
  }

  /** 검색 기준과 결과 목록으로 커서 페이징 정보 생성 */
  public CursorPageable getCursorPageable(
      AlcoholSearchCriteria criteriaDto,
      List<AlcoholsSearchItem> fetch,
      Long cursor,
      Long pageSize) {
    boolean hasNext = isHasNext(criteriaDto, fetch);
    return CursorPageable.builder()
        .currentCursor(cursor)
        .cursor(cursor + pageSize)
        .pageSize(pageSize)
        .hasNext(hasNext)
        .build();
  }

  /** 다음 페이지 존재 여부 확인 및 초과 항목 제거 */
  public boolean isHasNext(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchItem> fetch) {
    boolean hasNext = fetch.size() > criteriaDto.pageSize();

    if (hasNext) {
      fetch.remove(fetch.size() - 1);
    }
    return hasNext;
  }

  /** 정렬 조건에 따른 OrderSpecifier 생성 */
  public OrderSpecifier<?> sortBy(SearchSortType searchSortType, SortOrder sortOrder) {
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg();
    NumberExpression<Long> reviewCount = review.id.countDistinct();
    NumberExpression<Long> pickCount = picks.id.countDistinct();
    return switch (searchSortType) {
      case POPULAR ->
          sortOrder == SortOrder.DESC
              ? avgRating.add(reviewCount).desc()
              : avgRating.add(reviewCount).asc();
      case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
      case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
      case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
    };
  }

  /** 랜덤 정렬 조건 생성 */
  public OrderSpecifier<?> sortByRandom() {
    return Expressions.numberTemplate(Double.class, "function('rand')").asc();
  }

  /** 이름 포함 여부 조건 생성 */
  public BooleanExpression eqName(String name) {
    if (StringUtils.isNullOrEmpty(name)) return null;

    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 카테고리 일치 여부 조건 생성 */
  public BooleanExpression eqCategory(AlcoholCategoryGroup category) {
    if (Objects.isNull(category)) return null;

    return alcohol.categoryGroup.stringValue().like("%" + category + "%");
  }

  /** 지역 일치 여부 조건 생성 */
  public BooleanExpression eqRegion(Long regionId) {
    if (regionId == null) return null;

    return alcohol.region.id.eq(regionId);
  }

  /** 사용자가 주류에 준 평점 조회 (QueryDSL 경로 버전) */
  public NumberExpression<Double> myRating(NumberPath<Long> alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(rating.ratingPoint.rating)
                .from(rating)
                .where(rating.id.alcoholId.eq(alcoholId).and(rating.id.userId.eq(userId)))
                .limit(1))
        .coalesce(0.0)
        .castToNum(Double.class)
        .as("myRating");
  }

  /** 사용자가 주류에 작성한 리뷰들의 평균 평점 계산 (QueryDSL 경로 버전) */
  public NumberExpression<Double> averageReviewRating(NumberPath<Long> alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(
                    review
                        .reviewRating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0))
                .from(review)
                .where(review.alcoholId.eq(alcoholId).and(review.userId.eq(userId))))
        .castToNum(Double.class)
        .as("myAvgRating");
  }

  /** 특정 주류가 사용자에 의해 찜되었는지 확인 (QueryDSL 경로 버전) */
  public BooleanExpression isPickedSubquery(NumberPath<Long> alcoholId, Long userId) {
    if (userId == -1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(picks)
                .where(
                    picks
                        .alcoholId
                        .eq(alcoholId)
                        .and(picks.userId.eq(userId))
                        .and(picks.status.eq(PICK)))
                .exists())
        .as("isPicked");
  }

  /** 다수 키워드에 대한 검색 조건 생성 */
  public BooleanExpression keywordsMatch(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return null;
    }

    BooleanExpression finalCondition = null;

    // 각 키워드에 대해 개별 조건을 생성하고 AND 연산으로 결합
    for (String keyword : keywords) {
      if (StringUtils.isNullOrEmpty(keyword)) {
        continue; // 빈 키워드는 건너뛰기
      }
      // 현재 키워드에 대한 기본 필드 검색 조건
      BooleanExpression basicCondition =
          alcohol
              .korName
              .likeIgnoreCase("%" + keyword + "%")
              .or(alcohol.engName.likeIgnoreCase("%" + keyword + "%"))
              .or(alcohol.korCategory.likeIgnoreCase("%" + keyword + "%"))
              .or(alcohol.engCategory.likeIgnoreCase("%" + keyword + "%"))
              .or(region.korName.likeIgnoreCase("%" + keyword + "%"))
              .or(region.engName.likeIgnoreCase("%" + keyword + "%"));

      // 현재 키워드에 대한 테이스팅 태그 검색 조건
      BooleanExpression tastingTagCondition =
          JPAExpressions.selectOne()
              .from(alcoholsTastingTags)
              .join(tastingTag)
              .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
              .where(
                  alcoholsTastingTags.alcohol.id.eq(alcohol.id),
                  tastingTag
                      .korName
                      .likeIgnoreCase("%" + keyword + "%")
                      .or(tastingTag.engName.likeIgnoreCase("%" + keyword + "%")))
              .exists();

      // 현재 키워드에 대한 조건 (기본 필드 또는 테이스팅 태그)
      BooleanExpression keywordCondition = basicCondition.or(tastingTagCondition);

      // 결과 조건에 AND로 결합
      if (finalCondition == null) {
        finalCondition = keywordCondition;
      } else {
        finalCondition = finalCondition.and(keywordCondition);
      }
    }

    return finalCondition;
  }

  /** 단건 키워드 검색 조건 생성 */
  public BooleanExpression keywordMatch(String keyword) {
    if (StringUtils.isNullOrEmpty(keyword)) return null;

    // 기본 검색 조건 (이름, 카테고리)
    BooleanExpression basicSearch =
        alcohol
            .korName
            .like("%" + keyword + "%")
            .or(alcohol.engName.like("%" + keyword + "%"))
            .or(alcohol.korCategory.like("%" + keyword + "%"))
            .or(alcohol.engCategory.like("%" + keyword + "%"));

    // 미리 정의된 태그 검색 조건
    BooleanExpression predefinedTagSearch = getTastingTagsExpression(keyword);

    // **동적 태그 검색 조건 추가** (현재 누락된 부분)
    BooleanExpression dynamicTagSearch =
        JPAExpressions.selectOne()
            .from(alcoholsTastingTags)
            .join(tastingTag)
            .on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
            .where(
                alcoholsTastingTags.alcohol.id.eq(alcohol.id),
                tastingTag
                    .korName
                    .like("%" + keyword + "%")
                    .or(tastingTag.engName.like("%" + keyword + "%")))
            .exists();

    // 모든 조건을 OR로 결합
    BooleanExpression result = basicSearch.or(dynamicTagSearch);
    if (predefinedTagSearch != null) {
      result = result.or(predefinedTagSearch);
    }

    return result;
  }

  public BooleanExpression getTastingTagsExpression(String keyword) {
    var tagMapping = KeywordTagMapping.findByKeyword(keyword);

    if (tagMapping.isPresent()) {
      List<Long> tagIds = tagMapping.get().getIncludeTags();

      if (tagIds != null && !tagIds.isEmpty()) {
        return JPAExpressions.selectOne()
            .from(alcoholsTastingTags)
            .where(
                alcoholsTastingTags.alcohol.id.eq(alcohol.id),
                alcoholsTastingTags.tastingTag.id.in(tagIds))
            .exists();
      }
    }
    return null;
  }
}
