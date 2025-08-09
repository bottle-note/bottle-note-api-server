package app.bottlenote.user.dto.response;

import java.util.Collections;
import java.util.List;

public record MyBottleResponse(
    Long userId, Boolean isMyPage, Long totalCount, List<?> myBottleList) {

  public static MyBottleResponse create(
      Long userId, Boolean isMyPage, Long totalCount, List<?> myBottleList) {
    return new MyBottleResponse(
        userId,
        isMyPage,
        totalCount,
        myBottleList != null ? myBottleList : Collections.emptyList());
  }

  public record BaseMyBottleInfo(
      Long alcoholId,
      String alcoholKorName,
      String alcoholEngName,
      String korCategoryName,
      String imageUrl,
      Boolean isHot5) {}
}
