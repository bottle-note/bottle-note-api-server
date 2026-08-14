package app.bottlenote.user.dto.dsl;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.request.MyBottleRequest;

public record MyBottlePageableCriteria(
    Long userId,
    String keyword,
    Long regionId,
    MyBottleSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size,
    Long currentUserId) {
  public static MyBottlePageableCriteria of(
      MyBottleRequest request, Long userId, Long currentUserId) {
    return new MyBottlePageableCriteria(
        userId,
        request.keyword(),
        request.regionId(),
        request.sortType(),
        request.sortOrder(),
        request.cursor(),
        request.size(),
        currentUserId);
  }

  public String context(MyBottleType tab) {
    return "mybottle:"
        + tab
        + ":"
        + userId
        + ":"
        + currentUserId
        + ":"
        + keyword
        + ":"
        + regionId
        + ":"
        + sortType
        + ":"
        + sortOrder;
  }
}
