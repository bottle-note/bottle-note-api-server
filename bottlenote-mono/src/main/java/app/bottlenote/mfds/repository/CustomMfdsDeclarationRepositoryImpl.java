package app.bottlenote.mfds.repository;

import static app.bottlenote.mfds.domain.QMfdsDeclaration.mfdsDeclaration;

import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** QueryDSL id-desc keyset 조회. cursor&gt;0이면 id &lt; cursor, limit = pageSize + 1. */
@RequiredArgsConstructor
public class CustomMfdsDeclarationRepositoryImpl implements CustomMfdsDeclarationRepository {

  private final JPAQueryFactory queryFactory;
  private final MfdsDeclarationQuerySupporter supporter;

  @Override
  public List<MfdsDeclaration> searchByCriteria(MfdsDeclarationSearchCriteria criteria) {
    return queryFactory
        .selectFrom(mfdsDeclaration)
        .where(
            supporter.eqNormalizationStatus(criteria.normalizationStatus()),
            supporter.alcoholMatched(criteria.alcoholMatched()),
            supporter.eqAlcoholMatchDecision(criteria.alcoholMatchDecision()),
            supporter.eqImporterId(criteria.importerId()),
            supporter.keywordContains(criteria.keyword()),
            supporter.ltCursor(criteria.hasCursor(), criteria.cursor()))
        .orderBy(mfdsDeclaration.id.desc())
        .limit(criteria.fetchLimit())
        .fetch();
  }

  @Override
  public long countByCriteria(MfdsDeclarationSearchCriteria criteria) {
    Long total =
        queryFactory
            .select(mfdsDeclaration.count())
            .from(mfdsDeclaration)
            .where(
                supporter.eqNormalizationStatus(criteria.normalizationStatus()),
                supporter.alcoholMatched(criteria.alcoholMatched()),
                supporter.eqAlcoholMatchDecision(criteria.alcoholMatchDecision()),
                supporter.eqImporterId(criteria.importerId()),
                supporter.keywordContains(criteria.keyword()))
            .fetchOne();
    return total != null ? total : 0L;
  }
}
