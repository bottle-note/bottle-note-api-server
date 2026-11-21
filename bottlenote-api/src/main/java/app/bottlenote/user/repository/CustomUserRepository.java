package app.bottlenote.user.repository;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

public interface CustomUserRepository {

  MyPageResponse getMyPage(Long userId, Long currentUserId);

  KeysetPageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria criteria);

  KeysetPageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria criteria);

  KeysetPageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria criteria);
}
