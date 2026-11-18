package app.bottlenote.user.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import lombok.Builder;

public record MyBottleRequest(
    String keyword,
    Long regionId,
    MyBottleSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 50;
  public static final int MAX_SIZE = 100;

  @Builder
  public MyBottleRequest {
    sortType = sortType != null ? sortType : MyBottleSortType.LATEST;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
