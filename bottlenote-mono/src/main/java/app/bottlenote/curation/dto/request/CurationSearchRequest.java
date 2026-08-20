package app.bottlenote.curation.dto.request;

import app.bottlenote.curation.constant.CurationSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import lombok.Builder;

public record CurationSearchRequest(
    String keyword,
    String code,
    Boolean isActive,
    Integer page,
    Integer size,
    CurationSortType sortType,
    SortOrder sortOrder) {

  public CurationSearchRequest(String keyword, Boolean isActive, Integer page, Integer size) {
    this(keyword, null, isActive, page, size, null, null);
  }

  public CurationSearchRequest(String keyword, String code, Boolean isActive, Integer page, Integer size) {
    this(keyword, code, isActive, page, size, null, null);
  }

  @Builder
  public CurationSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
    sortType = sortType != null ? sortType : CurationSortType.EXPOSURE_START_DATE;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
  }
}
