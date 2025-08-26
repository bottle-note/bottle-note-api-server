package app.bottlenote.user.domain;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import java.util.List;
import java.util.Optional;

/** 유저 정보에 관한 질의에 대한 애그리거트를 정의합니다. */
public interface UserRepository {

  User save(User user);

  Optional<User> findById(Long usrId);

  List<User> findAll();

  boolean existsByUserId(Long userId);

  boolean existsByNickName(String nickname);

  MyPageResponse getMyPage(Long userId, Long currentUserId);

  PageResponse<MyBottleResponse> getReviewMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getRatingMyBottle(MyBottlePageableCriteria criteria);

  PageResponse<MyBottleResponse> getPicksMyBottle(MyBottlePageableCriteria criteria);
}
