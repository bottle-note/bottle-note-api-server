package app.bottlenote.user.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.AdminUserSortType;
import app.bottlenote.user.constant.UserStatus;
import lombok.Builder;

/**
 * @param keyword 검색 키워드 (닉네임/이메일)
 * @param status 유저 상태 필터 (ACTIVE/DELETED)
 * @param sortType 정렬 기준
 * @param sortOrder 정렬 방향
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기
 */
public record AdminUserSearchRequest(
    String keyword,
    UserStatus status,
    AdminUserSortType sortType,
    SortOrder sortOrder,
    Integer page,
    Integer size) {

  @Builder
  public AdminUserSearchRequest {
    sortType = sortType != null ? sortType : AdminUserSortType.CREATED_AT;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
