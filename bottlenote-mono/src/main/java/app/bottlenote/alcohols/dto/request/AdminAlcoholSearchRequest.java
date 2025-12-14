package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import lombok.Builder;

/**
 * @param keyword 이름 검색 (한글/영문)
 * @param category 카테고리 그룹
 * @param regionId 지역 ID
 * @param sortType 정렬 기준
 * @param sortOrder 정렬 방향
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기
 */
public record AdminAlcoholSearchRequest(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Integer page,
    Integer size) {
  @Builder
  public AdminAlcoholSearchRequest {
    sortType = sortType != null ? sortType : SearchSortType.POPULAR;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
