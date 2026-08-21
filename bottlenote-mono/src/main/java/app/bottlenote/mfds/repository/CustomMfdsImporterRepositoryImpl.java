package app.bottlenote.mfds.repository;

import static app.bottlenote.mfds.domain.QMfdsImporter.mfdsImporter;

import app.bottlenote.mfds.domain.MfdsImporter;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** QueryDSL id-desc keyset 조회. cursor&gt;0이면 id &lt; cursor, limit = pageSize + 1. */
@RequiredArgsConstructor
public class CustomMfdsImporterRepositoryImpl implements CustomMfdsImporterRepository {

  private final JPAQueryFactory queryFactory;
  private final MfdsImporterQuerySupporter supporter;

  @Override
  public List<MfdsImporter> searchByCriteria(MfdsImporterSearchCriteria criteria) {
    return queryFactory
        .selectFrom(mfdsImporter)
        .where(
            supporter.eqAdminStatus(criteria.adminStatus()),
            supporter.keywordContains(criteria.keyword()),
            supporter.ltCursor(criteria.hasCursor(), criteria.cursor()))
        .orderBy(mfdsImporter.id.desc())
        .limit(criteria.fetchLimit())
        .fetch();
  }

  @Override
  public long countByCriteria(MfdsImporterSearchCriteria criteria) {
    Long total =
        queryFactory
            .select(mfdsImporter.count())
            .from(mfdsImporter)
            .where(
                supporter.eqAdminStatus(criteria.adminStatus()),
                supporter.keywordContains(criteria.keyword()))
            .fetchOne();
    return total != null ? total : 0L;
  }
}
