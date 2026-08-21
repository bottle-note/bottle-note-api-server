package app.bottlenote.mfds.dto.request;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import app.bottlenote.mfds.dto.dsl.MfdsImporterSearchCriteria;

/**
 * 수입사 목록 검색 요청.
 *
 * @param adminStatus 관리 상태 필터
 * @param keyword 수입사명·인허가 번호·업소 코드 검색어
 * @param cursor 마지막으로 받은 수입사 ID (최초 페이지는 생략)
 * @param pageSize 페이지 크기
 */
public record MfdsImporterSearchRequest(
    MfdsImporterAdminStatus adminStatus, String keyword, Long cursor, Long pageSize) {

  public MfdsImporterSearchCriteria toCriteria() {
    return new MfdsImporterSearchCriteria(adminStatus, keyword, cursor, pageSize);
  }
}
