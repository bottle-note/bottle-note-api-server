package app.bottlenote.curation.repository;

import static app.bottlenote.curation.domain.QCuration.curation;

import app.bottlenote.curation.constant.CurationSortType;
import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomCurationFeedRepositoryImpl implements CustomCurationFeedRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Long> findFeedCandidateIds(CurationFeedSearchCriteria criteria) {
    if (criteria.specIds().isEmpty()) {
      return List.of();
    }
    return queryFactory
        .select(curation.id)
        .from(curation)
        .where(
            curation.isActive.isTrue(),
            curation
                .exposureStartDate
                .isNull()
                .or(curation.exposureStartDate.loe(criteria.today())),
            curation.exposureEndDate.isNull().or(curation.exposureEndDate.goe(criteria.today())),
            curation.specId.in(criteria.specIds()),
            matchesKeyword(criteria),
            seek(criteria))
        .orderBy(orderBy(criteria))
        .limit(criteria.fetchSize())
        .fetch();
  }

  private static OrderSpecifier<?>[] orderBy(CurationFeedSearchCriteria criteria) {
    if (criteria.sortType() == CurationSortType.DISPLAY_ORDER) {
      return new OrderSpecifier<?>[] {
        criteria.sortOrder().resolve(curation.displayOrder),
        criteria.sortOrder().resolve(curation.id)
      };
    }
    return new OrderSpecifier<?>[] {
      new CaseBuilder().when(curation.exposureStartDate.isNull()).then(1).otherwise(0).asc(),
      criteria.sortOrder().resolve(curation.exposureStartDate),
      criteria.sortOrder().resolve(curation.id)
    };
  }

  private static BooleanExpression seek(CurationFeedSearchCriteria criteria) {
    if (criteria.lastId() == null) {
      return null;
    }
    if (criteria.sortType() == CurationSortType.DISPLAY_ORDER) {
      return orderedAfter(
          curation.displayOrder,
          criteria.lastDisplayOrder(),
          criteria.lastId(),
          criteria.sortOrder());
    }
    if (criteria.lastExposureStartDate() == null) {
      return curation
          .exposureStartDate
          .isNull()
          .and(idAfter(criteria.lastId(), criteria.sortOrder()));
    }
    return orderedAfter(
            curation.exposureStartDate,
            criteria.lastExposureStartDate(),
            criteria.lastId(),
            criteria.sortOrder())
        .or(curation.exposureStartDate.isNull());
  }

  private static BooleanExpression orderedAfter(
      NumberPath<Integer> field, Integer lastValue, Long lastId, SortOrder sortOrder) {
    BooleanExpression primaryAfter =
        sortOrder == SortOrder.ASC ? field.gt(lastValue) : field.lt(lastValue);
    return primaryAfter.or(field.eq(lastValue).and(idAfter(lastId, sortOrder)));
  }

  private static BooleanExpression orderedAfter(
      DatePath<LocalDate> field, LocalDate lastValue, Long lastId, SortOrder sortOrder) {
    BooleanExpression primaryAfter =
        sortOrder == SortOrder.ASC ? field.gt(lastValue) : field.lt(lastValue);
    return primaryAfter.or(field.eq(lastValue).and(idAfter(lastId, sortOrder)));
  }

  private static BooleanExpression idAfter(Long lastId, SortOrder sortOrder) {
    return sortOrder == SortOrder.ASC ? curation.id.gt(lastId) : curation.id.lt(lastId);
  }

  private BooleanExpression matchesKeyword(CurationFeedSearchCriteria criteria) {
    if (criteria.keyword() == null || criteria.keyword().isBlank()) {
      return null;
    }
    String keyword = criteria.keyword().trim();
    BooleanExpression matchesCuration =
        curation.name.contains(keyword).or(curation.description.contains(keyword));
    return criteria.keywordMatchedSpecIds().isEmpty()
        ? matchesCuration
        : matchesCuration.or(curation.specId.in(criteria.keywordMatchedSpecIds()));
  }
}
