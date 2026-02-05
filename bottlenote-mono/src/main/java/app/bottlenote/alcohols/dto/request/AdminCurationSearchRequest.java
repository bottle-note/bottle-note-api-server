package app.bottlenote.alcohols.dto.request;

import lombok.Builder;

/**
 * Admin 큐레이션 목록 검색 요청
 *
 * @param keyword 큐레이션 이름 검색어
 * @param isActive 활성화 상태 필터 (null: 전체, true: 활성, false: 비활성)
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 */
public record AdminCurationSearchRequest(
    String keyword, Boolean isActive, Integer page, Integer size) {

  @Builder
  public AdminCurationSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
