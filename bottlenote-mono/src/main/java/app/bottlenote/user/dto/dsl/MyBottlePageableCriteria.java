package app.bottlenote.user.dto.dsl;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.request.MyBottleRequest;
import java.util.Set;

public record MyBottlePageableCriteria(
    Long userId,
    String keyword,
    Long regionId,
    MyBottleSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size,
    Long currentUserId,
    Set<Long> hotAlcoholIds) {

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
        currentUserId,
        Set.of());
  }

  public MyBottlePageableCriteria withHotAlcoholIds(Set<Long> alcoholIds) {
    return new MyBottlePageableCriteria(
        userId, keyword, regionId, sortType, sortOrder, cursor, size, currentUserId, alcoholIds);
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
