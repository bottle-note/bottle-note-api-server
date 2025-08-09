package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

public interface CustomUserRepository {

  MyPageResponse getMyPage(Long userId, Long currentUserId);

  PageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria criteria);
}
