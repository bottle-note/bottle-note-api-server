package app.bottlenote.user.service;

import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.event.payload.FollowActivityEvent;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

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

    boolean activated =
        request.status() == FollowStatus.FOLLOWING
            && (follow.getId() == null || follow.getStatus() != FollowStatus.FOLLOWING);
    follow.updateStatus(request.status());
    followRepository.save(follow);
    if (activated) {
      eventPublisher.publishEvent(
          new FollowActivityEvent(follow.getId(), currentUserId, followUserId));
    }

    return FollowUpdateResponse.builder()
        .status(follow.getStatus())
        .followUserId(followUserId)
        .nickName(targetUser.getNickName())
        .imageUrl(targetUser.getImageUrl())
        .build();
  }
}
