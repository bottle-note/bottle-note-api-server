package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.dto.dsl.MfdsDeclarationSearchCriteria;

/**
 * 수입 신고 목록 검색 요청.
 *
 * @param normalizationStatus 정규화 상태 필터
 * @param alcoholMatched 주류 매칭 완료 여부 필터 (true: 매칭됨, false: 미매칭)
 * @param alcoholMatchDecision 주류 매칭 결정 근거 필터 (CANDIDATE, MANUAL, AUTO)
 * @param importerId 연결된 수입사 ID 필터
 * @param keyword 제품 검색 키·수입신고번호 검색어
 * @param cursor 마지막으로 받은 신고 데이터 ID (최초 페이지는 생략)
 * @param pageSize 페이지 크기
 */
public record MfdsDeclarationSearchRequest(
    MfdsNormalizationStatus normalizationStatus,
    Boolean alcoholMatched,
    MfdsMatchSelectionSource alcoholMatchDecision,
    Long importerId,
    String keyword,
    Long cursor,
    Long pageSize) {

  public MfdsDeclarationSearchCriteria toCriteria() {
    return new MfdsDeclarationSearchCriteria(
        normalizationStatus,
        alcoholMatched,
        alcoholMatchDecision,
        importerId,
        keyword,
        cursor,
        pageSize);
  }
}
