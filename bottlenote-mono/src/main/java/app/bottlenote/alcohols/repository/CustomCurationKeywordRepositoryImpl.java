package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QCurationKeyword.curationKeyword;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.domain.CurationKeyword;
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class CustomCurationKeywordRepositoryImpl implements CustomCurationKeywordRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorResponse<CurationKeywordResponse> searchCurationKeywords(
      String keyword, Long alcoholId, Long cursor, Integer pageSize) {
    List<CurationKeywordResponse> results =
        queryFactory
            .select(
                Projections.fields(
                    CurationKeywordResponse.class,
                    curationKeyword.id.as("id"),
                    curationKeyword.name.as("name"),
                    curationKeyword.description.as("description"),
                    curationKeyword.coverImageUrl.as("coverImageUrl"),
                    curationKeyword.alcoholIds.size().as("alcoholCount"),
                    curationKeyword.displayOrder.as("displayOrder")))
            .from(curationKeyword)
            .where(
                curationKeyword.isActive.isTrue(),
                keywordContains(keyword),
                alcoholIdIn(alcoholId),
                curationKeyword.id.gt(cursor))
            .orderBy(curationKeyword.displayOrder.asc(), curationKeyword.id.desc())
            .limit(pageSize + 1)
            .fetch();

    CursorPageable pageable = CursorPageable.of(results, cursor, pageSize);
    List<CurationKeywordResponse> content =
        results.size() > pageSize ? results.subList(0, pageSize) : results;

    return CursorResponse.of(content, pageable);
  }

  @Override
  public CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Integer pageSize) {
    CurationKeyword curation =
        queryFactory
            .selectFrom(curationKeyword)
            .where(curationKeyword.id.eq(curationId))
            .fetchOne();

    if (curation == null || curation.getAlcoholIds().isEmpty()) {
      return CursorResponse.of(
          List.of(),
          CursorPageable.builder()
              .currentCursor(cursor)
              .cursor(cursor)
              .pageSize((long) pageSize)
              .hasNext(false)
              .build());
    }

    List<Long> alcoholIdsList = curation.getAlcoholIds().stream().toList();

    List<AlcoholsSearchItem> results =
        queryFactory
            .select(
                Projections.fields(
                    AlcoholsSearchItem.class,
                    alcohol.id.as("alcoholId"),
                    alcohol.korName.as("korName"),
                    alcohol.engName.as("engName"),
                    alcohol.korCategory.as("korCategoryName"),
                    alcohol.engCategory.as("engCategoryName"),
                    alcohol.imageUrl.as("imageUrl"),
                    rating
                        .ratingPoint
                        .rating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0)
                        .as("rating"),
                    rating.id.countDistinct().as("ratingCount"),
                    review.id.countDistinct().as("reviewCount"),
                    picks.id.countDistinct().as("pickCount"),
                    Expressions.asBoolean(false).as("isPicked")))
            .from(alcohol)
            .leftJoin(rating)
            .on(alcohol.id.eq(rating.id.alcoholId))
            .leftJoin(review)
            .on(alcohol.id.eq(review.alcoholId))
            .leftJoin(picks)
            .on(alcohol.id.eq(picks.alcoholId))
            .where(alcohol.id.in(alcoholIdsList), alcohol.id.gt(cursor))
            .groupBy(
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.imageUrl)
            // .orderBy()
            .limit(pageSize + 1)
            .fetch();

    CursorPageable pageable = CursorPageable.of(results, cursor, pageSize);
    List<AlcoholsSearchItem> content =
        results.size() > pageSize ? results.subList(0, pageSize) : results;

    return CursorResponse.of(content, pageable);
  }

  @Override
  public java.util.Optional<java.util.Set<Long>> findAlcoholIdsByKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return java.util.Optional.empty();
    }

    CurationKeyword result =
        queryFactory
            .selectFrom(curationKeyword)
            .where(
                curationKeyword.isActive.isTrue(), curationKeyword.name.containsIgnoreCase(keyword))
            .fetchFirst();

    return java.util.Optional.ofNullable(result).map(CurationKeyword::getAlcoholIds);
  }

  private BooleanExpression keywordContains(String keyword) {
    return keyword != null && !keyword.isBlank()
        ? curationKeyword.name.containsIgnoreCase(keyword)
        : null;
  }

  private BooleanExpression alcoholIdIn(Long alcoholId) {
    if (alcoholId == null) {
      return null;
    }

    return Expressions.numberTemplate(
            Long.class,
            "CASE WHEN {0} MEMBER OF {1} THEN 1 ELSE 0 END",
            alcoholId,
            curationKeyword.alcoholIds)
        .eq(1L);
  }

  @Override
  public Page<AdminCurationListResponse> searchForAdmin(
      AdminCurationSearchRequest request, Pageable pageable) {

    List<AdminCurationListResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminCurationListResponse.class,
                    curationKeyword.id,
                    curationKeyword.name,
                    curationKeyword.alcoholIds.size(),
                    curationKeyword.displayOrder,
                    curationKeyword.isActive,
                    curationKeyword.createAt))
            .from(curationKeyword)
            .where(keywordContains(request.keyword()), isActiveEq(request.isActive()))
            .orderBy(curationKeyword.displayOrder.asc(), curationKeyword.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory
            .select(curationKeyword.count())
            .from(curationKeyword)
            .where(keywordContains(request.keyword()), isActiveEq(request.isActive()))
            .fetchOne();

    return new PageImpl<>(content, pageable, total != null ? total : 0L);
  }

  private BooleanExpression isActiveEq(Boolean isActive) {
    return isActive != null ? curationKeyword.isActive.eq(isActive) : null;
  }
}
