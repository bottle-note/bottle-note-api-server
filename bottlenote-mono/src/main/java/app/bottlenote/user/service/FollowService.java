package app.bottlenote.user.service;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.facade.FollowFacade;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FollowService implements FollowFacade {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final HmacCursorCodec cursorCodec;

  @Transactional
  public FollowUpdateResponse updateFollowStatus(FollowUpdateRequest request, Long currentUserId) {
    Long followUserId = request.followUserId();

    if (currentUserId.equals(followUserId)) {
      throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
    }

    Follow follow =
        followRepository
            .findByUserIdAndFollowUserId(currentUserId, followUserId)
            .orElseGet(
                () -> {
                  User user =
                      userRepository
                          .findById(currentUserId)
                          .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

                  return Follow.builder()
                      .userId(user.getId())
                      .targetUserId(request.followUserId())
                      .build();
                });
    User targetUser =
        userRepository
            .findById(followUserId)
            .orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

    follow.updateStatus(request.status());
    followRepository.save(follow);

    return FollowUpdateResponse.builder()
        .status(follow.getStatus())
        .followUserId(followUserId)
        .nickName(targetUser.getNickName())
        .imageUrl(targetUser.getImageUrl())
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<FollowingSearchResponse> getFollowingList(
      Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

    if (!userRepository.existsByUserId(currentUserId)) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    return followRepository.getFollowingList(userId, toCriteria(pageableRequest, userId, true));
  }

  @Transactional(readOnly = true)
  public PageResponse<FollowerSearchResponse> getFollowerList(
      Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

    if (!userRepository.existsByUserId(currentUserId)) {
      throw new UserException(UserExceptionCode.USER_NOT_FOUND);
    }

    return followRepository.getFollowerList(userId, toCriteria(pageableRequest, userId, false));
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
    return followRepository.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
  }
}
