package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordDto;
import app.bottlenote.global.service.cursor.CursorResponse;

public interface CustomCurationKeywordRepository {

  /**
   * 큐레이션 키워드 검색
   *
   * @param keyword 큐레이션 키워드 이름 검색 (부분 일치)
   * @param alcoholId 위스키 ID로 검색 (해당 ID가 포함된 큐레이션 조회)
   * @param cursor 커서 페이징
   * @param pageSize 페이지 크기
   * @return 큐레이션 키워드 목록
   */
  CursorResponse<CurationKeywordDto> searchCurationKeywords(
      String keyword, Long alcoholId, Long cursor, Integer pageSize);

  /**
   * 특정 큐레이션의 위스키 목록 조회
   *
   * @param curationId 큐레이션 ID
   * @param cursor 커서 페이징
   * @param pageSize 페이지 크기
   * @return 위스키 목록
   */
  CursorResponse<AlcoholsSearchItem> getCurationAlcohols(
      Long curationId, Long cursor, Integer pageSize);
}
