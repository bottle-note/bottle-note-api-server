package app.bottlenote.mfds.repository;

import static app.bottlenote.mfds.domain.QMfdsDeclaration.mfdsDeclaration;

import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
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

  public BooleanExpression eqAlcoholMatchDecision(MfdsMatchSelectionSource decision) {
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
}
