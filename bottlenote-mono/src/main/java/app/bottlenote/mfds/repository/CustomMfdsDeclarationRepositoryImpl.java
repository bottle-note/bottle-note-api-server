package app.bottlenote.mfds.repository;

import static app.bottlenote.mfds.domain.QMfdsDeclaration.mfdsDeclaration;
import static app.bottlenote.mfds.domain.QMfdsImporter.mfdsImporter;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;
import app.bottlenote.mfds.dto.dsl.MfdsPublicAlcoholSearchCriteria;
import app.bottlenote.mfds.dto.response.MfdsPublicCountryItem;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
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

  @Override
  public List<MfdsDeclaration> searchPublicAlcohols(MfdsPublicAlcoholSearchCriteria criteria) {
    JPAQuery<MfdsDeclaration> query = queryFactory.selectFrom(mfdsDeclaration);
    if (hasSearchTokens(criteria)) {
      query.leftJoin(mfdsImporter).on(mfdsDeclaration.importerId.eq(mfdsImporter.id));
    }
    return query
        .where(
            supporter.eqNormalizationStatus(MfdsNormalizationStatus.NORMALIZED),
            supporter.eqAlcoholNameKo(criteria.alcoholNameKo()),
            supporter.eqSelectedAlcoholId(criteria.alcoholId()),
            supporter.eqImporterId(criteria.importerId()),
            supporter.eqExportCountry(criteria.exportCountry()),
            supporter.eqAlcoholCategoryKo(criteria.alcoholCategoryKo()),
            supporter.processedDateFrom(criteria.processedDateFrom()),
            supporter.processedDateTo(criteria.processedDateTo()),
            supporter.publicSearchTokensMatch(criteria.searchTokens()),
            supporter.processedDateCursor(criteria.cursorProcessedDate(), criteria.cursorId()))
        .orderBy(mfdsDeclaration.processedDate.desc(), mfdsDeclaration.id.desc())
        .limit(criteria.fetchLimit())
        .fetch();
  }

  @Override
  public List<MfdsPublicCountryItem> findExportCountries() {
    return queryFactory
        .select(
            Projections.constructor(
                MfdsPublicCountryItem.class,
                mfdsDeclaration.exportCountryAlpha2,
                mfdsDeclaration.exportCountryNameKo.max(),
                mfdsDeclaration.exportCountryNameEn.max()))
        .from(mfdsDeclaration)
        .where(
            supporter.eqNormalizationStatus(MfdsNormalizationStatus.NORMALIZED),
            mfdsDeclaration.exportCountryAlpha2.isNotNull())
        .groupBy(mfdsDeclaration.exportCountryAlpha2)
        .orderBy(mfdsDeclaration.exportCountryNameKo.max().asc())
        .fetch();
  }

  private static boolean hasSearchTokens(MfdsPublicAlcoholSearchCriteria criteria) {
    return criteria.searchTokens() != null && !criteria.searchTokens().isEmpty();
  }
}
