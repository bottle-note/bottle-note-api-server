package app.bottlenote.curation.repository;

import static app.bottlenote.curation.domain.QCuration.curation;

import app.bottlenote.curation.dto.dsl.CurationFeedSearchCriteria;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
            displayOrderSeek(criteria))
        .orderBy(curation.displayOrder.asc(), curation.id.asc())
        .limit(criteria.fetchSize())
        .fetch();
  }

  private static BooleanExpression displayOrderSeek(CurationFeedSearchCriteria criteria) {
    if (criteria.lastId() == null) {
      return null;
    }
    int lastOrder = criteria.lastDisplayOrder() == null ? 0 : criteria.lastDisplayOrder();
    return curation
        .displayOrder
        .gt(lastOrder)
        .or(curation.displayOrder.eq(lastOrder).and(curation.id.gt(criteria.lastId())));
  }

  private BooleanExpression matchesKeyword(CurationFeedSearchCriteria criteria) {
    if (criteria.keyword() == null || criteria.keyword().isBlank()) {
      return null;
    }

    String keyword = criteria.keyword().trim();
    BooleanExpression matchesCuration =
        curation.name.contains(keyword).or(curation.description.contains(keyword));
    if (criteria.keywordMatchedSpecIds().isEmpty()) {
      return matchesCuration;
    }
    return matchesCuration.or(curation.specId.in(criteria.keywordMatchedSpecIds()));
  }
}
