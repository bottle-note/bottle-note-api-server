package app.bottlenote.user.repository;

import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

public interface CustomUserRepository {

	MyPageResponse getMyPage(Long userId, Long currentUserId);

	MyBottleResponse getReviewMyBottle(MyBottlePageableCriteria criteria);

	MyBottleResponse getRatingMyBottle(MyBottlePageableCriteria criteria);

}
