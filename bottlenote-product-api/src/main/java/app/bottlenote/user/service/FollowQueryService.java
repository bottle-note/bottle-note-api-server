package app.bottlenote.user.service;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import app.bottlenote.user.repository.CustomFollowRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowQueryService implements FollowFacade {

  private final CustomFollowRepository queryRepository;
  private final UserRepository userRepository;
  private final HmacCursorCodec cursorCodec;

  @Transactional(readOnly = true)
  public KeysetPageResponse<FollowingSearchResponse> getFollowingList(
      Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

    if (!userRepository.existsByUserId(currentUserId)) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    return queryRepository.getFollowingList(userId, toCriteria(pageableRequest, userId, true));
  }

  @Transactional(readOnly = true)
  public KeysetPageResponse<FollowerSearchResponse> getFollowerList(
      Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

    if (!userRepository.existsByUserId(currentUserId)) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    return queryRepository.getFollowerList(userId, toCriteria(pageableRequest, userId, false));
  }

  private FollowPageableCriteria toCriteria(
      FollowPageableRequest request, Long userId, boolean following) {
    if (request.cursor() == null) {
      return FollowPageableCriteria.first(request.size());
    }
    String context = (following ? "follow.following:" : "follow.follower:") + userId;
    var claims = cursorCodec.verify(request.cursor(), context);
    return new FollowPageableCriteria(
        request.size(), TimeIdCursor.time(claims), TimeIdCursor.id(claims));
  }

  @Override
  @Transactional(readOnly = true)
  public List<FriendItem> getTastingFriendsInfoList(
      Long alcoholId, Long userId, PageRequest pageRequest) {
    return queryRepository.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
  }
}
