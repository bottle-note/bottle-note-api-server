package app.bottlenote.mfds.repository;

import static app.bottlenote.global.search.SearchKeywordLikePattern.ESCAPE;
import static app.bottlenote.global.search.SearchKeywordLikePattern.contains;
import static app.bottlenote.mfds.domain.QMfdsDeclaration.mfdsDeclaration;
import static app.bottlenote.mfds.domain.QMfdsImporter.mfdsImporter;

import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.util.StringUtils;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** 신고 정제 데이터 목록 검색의 동적 조건 조립 헬퍼. null 반환 시 해당 조건은 제외된다. */
@Component
public class MfdsDeclarationQuerySupporter {

  public BooleanExpression eqNormalizationStatus(MfdsNormalizationStatus status) {
    return status != null ? mfdsDeclaration.normalizationStatus.eq(status) : null;
  }

  /** true: 주류 매칭 완료(selectedAlcoholId 존재), false: 미매칭, null: 조건 없음. */
  public BooleanExpression alcoholMatched(Boolean matched) {
    if (matched == null) {
      return null;
    }
    return matched
        ? mfdsDeclaration.selectedAlcoholId.isNotNull()
        : mfdsDeclaration.selectedAlcoholId.isNull();
  }

  public BooleanExpression eqAlcoholMatchDecision(String decision) {
    return decision != null ? mfdsDeclaration.alcoholMatchDecision.eq(decision) : null;
  }

  public BooleanExpression eqImporterId(Long importerId) {
    return importerId != null ? mfdsDeclaration.importerId.eq(importerId) : null;
  }

  /** 한글/영문 검색 키 또는 수입신고번호 부분 일치. */
  public BooleanExpression keywordContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return mfdsDeclaration
        .nameSearchKeyKo
        .containsIgnoreCase(keyword)
        .or(mfdsDeclaration.nameSearchKeyEn.containsIgnoreCase(keyword))
        .or(mfdsDeclaration.rcno.containsIgnoreCase(keyword));
  }

  /** id-desc keyset 커서 조건. 최초 페이지면 null. */
  public BooleanExpression ltCursor(boolean hasCursor, Long cursor) {
    return hasCursor ? mfdsDeclaration.id.lt(cursor) : null;
  }

  public BooleanExpression eqAlcoholNameKo(String alcoholNameKo) {
    return alcoholNameKo != null ? mfdsDeclaration.alcoholNameKo.eq(alcoholNameKo) : null;
  }

  public BooleanExpression eqSelectedAlcoholId(Long alcoholId) {
    return alcoholId != null ? mfdsDeclaration.selectedAlcoholId.eq(alcoholId) : null;
  }

  public BooleanExpression eqExportCountry(String exportCountry) {
    return exportCountry != null ? mfdsDeclaration.exportCountryAlpha2.eq(exportCountry) : null;
  }

  public BooleanExpression eqAlcoholCategoryKo(String alcoholCategoryKo) {
    return alcoholCategoryKo != null
        ? mfdsDeclaration.alcoholCategoryKo.eq(alcoholCategoryKo)
        : null;
  }

  public BooleanExpression processedDateFrom(LocalDate from) {
    return from != null ? mfdsDeclaration.processedDate.goe(from) : null;
  }

  public BooleanExpression processedDateTo(LocalDate to) {
    return to != null ? mfdsDeclaration.processedDate.loe(to) : null;
  }

  /** 토큰 간 AND, 공개 검색 필드 간 OR. 수입사명은 신고 표시명과 연결된 수입사명을 모두 본다. */
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
          like(mfdsDeclaration.rcno, token)
              .or(like(mfdsDeclaration.baseProductNameKo, token))
              .or(like(mfdsDeclaration.baseProductNameEn, token))
              .or(like(mfdsDeclaration.skuDisplayNameKo, token))
              .or(like(mfdsDeclaration.skuDisplayNameEn, token))
              .or(like(mfdsDeclaration.alcoholNameKo, token))
              .or(like(mfdsDeclaration.alcoholNameEn, token))
              .or(like(mfdsDeclaration.alcoholCategoryKo, token))
              .or(like(mfdsDeclaration.alcoholCategoryEn, token))
              .or(like(mfdsDeclaration.manufacturerName, token))
              .or(like(mfdsDeclaration.importerBaseName, token))
              .or(like(mfdsImporter.businessName, token));
      combined = combined == null ? tokenMatch : combined.and(tokenMatch);
    }
    return combined;
  }

  /**
   * 처리일자 내림차순, null은 마지막. 커서가 날짜가 있으면 그보다 과거이거나 같은 날짜에서 id가 더 작은 행, 날짜가 없으면 null 날짜의 더 작은 id만 남긴다.
   */
  public BooleanExpression processedDateCursor(LocalDate cursorProcessedDate, Long cursorId) {
    if (cursorId == null) {
      return null;
    }
    if (cursorProcessedDate == null) {
      return mfdsDeclaration.processedDate.isNull().and(mfdsDeclaration.id.lt(cursorId));
    }
    return mfdsDeclaration
        .processedDate
        .isNull()
        .or(mfdsDeclaration.processedDate.lt(cursorProcessedDate))
        .or(
            mfdsDeclaration
                .processedDate
                .eq(cursorProcessedDate)
                .and(mfdsDeclaration.id.lt(cursorId)));
  }

  private static BooleanExpression like(StringPath path, String token) {
    return path.likeIgnoreCase(contains(token), ESCAPE);
  }
}
