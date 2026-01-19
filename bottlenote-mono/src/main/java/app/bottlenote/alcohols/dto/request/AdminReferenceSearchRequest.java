package app.bottlenote.alcohols.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import lombok.Builder;

/**
 * 참조 데이터 (테이스팅 태그, 지역, 증류소) 검색용 공통 Request
 *
 * @param keyword 검색어
 * @param sortOrder 정렬 방향
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기
 */
public record AdminReferenceSearchRequest(
    String keyword, SortOrder sortOrder, Integer page, Integer size) {
  @Builder
  public AdminReferenceSearchRequest {
    sortOrder = sortOrder != null ? sortOrder : SortOrder.ASC;
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
