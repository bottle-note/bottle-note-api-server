package app.bottlenote.mfds.repository;

import static app.bottlenote.global.search.SearchKeywordLikePattern.ESCAPE;
import static app.bottlenote.global.search.SearchKeywordLikePattern.contains;
import static app.bottlenote.mfds.domain.QMfdsImporter.mfdsImporter;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.util.StringUtils;
import java.util.List;
import org.springframework.stereotype.Component;

/** 수입사 목록 검색의 동적 조건 조립 헬퍼. null 반환 시 해당 조건은 제외된다. */
@Component
public class MfdsImporterQuerySupporter {

  public BooleanExpression eqAdminStatus(MfdsImporterAdminStatus adminStatus) {
    return adminStatus != null ? mfdsImporter.adminStatus.eq(adminStatus) : null;
  }

  /** 수입사명, 인허가 번호 또는 공식 업소 코드 부분 일치. */
  public BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return mfdsImporter
        .businessName
        .containsIgnoreCase(keyword)
        .or(mfdsImporter.licenseNo.containsIgnoreCase(keyword))
        .or(mfdsImporter.officialBusinessCode.containsIgnoreCase(keyword));
  }

  /** id-desc keyset 커서 조건. 최초 페이지면 null. */
  public BooleanExpression ltCursor(boolean hasCursor, Long cursor) {
    return hasCursor ? mfdsImporter.id.lt(cursor) : null;
  }

  /** 토큰 간 AND, 수입사명·인허가 번호·업소 코드·대표자명 OR. */
  public BooleanExpression publicSearchTokensMatch(List<String> searchTokens) {
    if (searchTokens == null || searchTokens.isEmpty()) {
      return null;
    }
    BooleanExpression combined = null;
    for (String token : searchTokens) {
      if (StringUtils.isNullOrEmpty(token)) {
        continue;
      }
      BooleanExpression tokenMatch =
          mfdsImporter
              .businessName
              .likeIgnoreCase(contains(token), ESCAPE)
              .or(mfdsImporter.licenseNo.likeIgnoreCase(contains(token), ESCAPE))
              .or(mfdsImporter.officialBusinessCode.likeIgnoreCase(contains(token), ESCAPE))
              .or(mfdsImporter.representativeName.likeIgnoreCase(contains(token), ESCAPE));
      combined = combined == null ? tokenMatch : combined.and(tokenMatch);
    }
    return combined;
  }
}
