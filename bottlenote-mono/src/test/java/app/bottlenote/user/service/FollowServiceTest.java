package app.bottlenote.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.fixture.InMemoryFollowRepository;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("FollowService 단위 테스트")
class FollowServiceTest {

  InMemoryFollowRepository followRepository;
  InMemoryUserQueryRepository userRepository;
  FollowService followService;

  @BeforeEach
  void setUp() {
    followRepository = new InMemoryFollowRepository();
    userRepository = new InMemoryUserQueryRepository();
    followService = new FollowService(followRepository, userRepository);
  }

  // ========== updateFollowStatus ==========

  @Test
  @DisplayName("새로운 팔로우를 정상적으로 생성할 수 있다")
  void updateFollowStatus_신규_팔로우_생성() {
    // given
    User currentUser = createUser("current@example.com", "현재유저");
    User targetUser = createUser("target@example.com", "타겟유저");
    userRepository.save(currentUser);
    userRepository.save(targetUser);

    FollowUpdateRequest request =
        new FollowUpdateRequest(targetUser.getId(), FollowStatus.FOLLOWING);

    // when
    FollowUpdateResponse response = followService.updateFollowStatus(request, currentUser.getId());

    // then
    assertThat(response.getFollowUserId()).isEqualTo(targetUser.getId());
    assertThat(response.getNickName()).isEqualTo("타겟유저");
    assertThat(response.getMessage()).isEqualTo("성공적으로 팔로우 처리했습니다.");
  }

  @Test
  @DisplayName("기존 팔로우의 상태를 변경할 수 있다")
  void updateFollowStatus_팔로우_상태_변경() {
    // given
    User currentUser = createUser("current@example.com", "현재유저");
    User targetUser = createUser("target@example.com", "타겟유저");
    userRepository.save(currentUser);
    userRepository.save(targetUser);

    Follow existingFollow =
        Follow.builder()
            .userId(currentUser.getId())
            .targetUserId(targetUser.getId())
            .status(FollowStatus.FOLLOWING)
            .build();
    followRepository.save(existingFollow);

    FollowUpdateRequest request =
        new FollowUpdateRequest(targetUser.getId(), FollowStatus.UNFOLLOW);

    // when
    FollowUpdateResponse response = followService.updateFollowStatus(request, currentUser.getId());

    // then
    assertThat(response.getFollowUserId()).isEqualTo(targetUser.getId());
    assertThat(response.getMessage()).isEqualTo("성공적으로 팔로우 해제 처리했습니다.");
  }

  @Test
  @DisplayName("자기 자신을 팔로우하려 하면 CANNOT_FOLLOW_SELF 예외가 발생한다")
  void updateFollowStatus_자기_자신_팔로우_예외() {
    // given
    User user = createUser("user@example.com", "유저");
    userRepository.save(user);

    FollowUpdateRequest request = new FollowUpdateRequest(user.getId(), FollowStatus.FOLLOWING);

    // when & then
    assertThatThrownBy(() -> followService.updateFollowStatus(request, user.getId()))
        .isInstanceOf(FollowException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", FollowExceptionCode.CANNOT_FOLLOW_SELF);
  }

  @Test
  @DisplayName("존재하지 않는 현재 유저일 때 USER_NOT_FOUND 예외가 발생한다")
  void updateFollowStatus_현재_유저_없음_예외() {
    // given
    User targetUser = createUser("target@example.com", "타겟유저");
    userRepository.save(targetUser);

    Long nonExistentUserId = 999L;
    FollowUpdateRequest request =
        new FollowUpdateRequest(targetUser.getId(), FollowStatus.FOLLOWING);

    // when & then
    assertThatThrownBy(() -> followService.updateFollowStatus(request, nonExistentUserId))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", UserExceptionCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 타겟 유저일 때 FOLLOW_NOT_FOUND 예외가 발생한다")
  void updateFollowStatus_타겟_유저_없음_예외() {
    // given
    User currentUser = createUser("current@example.com", "현재유저");
    userRepository.save(currentUser);

    Long nonExistentTargetUserId = 999L;
    FollowUpdateRequest request =
        new FollowUpdateRequest(nonExistentTargetUserId, FollowStatus.FOLLOWING);

    // when & then
    assertThatThrownBy(() -> followService.updateFollowStatus(request, currentUser.getId()))
        .isInstanceOf(FollowException.class)
        .hasFieldOrPropertyWithValue("exceptionCode", FollowExceptionCode.FOLLOW_NOT_FOUND);
  }

  @Test
  @DisplayName("팔로우 후 다시 팔로우하면 상태가 유지된다")
  void updateFollowStatus_중복_팔로우_멱등성() {
    // given
    User currentUser = createUser("current@example.com", "현재유저");
    User targetUser = createUser("target@example.com", "타겟유저");
    userRepository.save(currentUser);
    userRepository.save(targetUser);

    FollowUpdateRequest request =
        new FollowUpdateRequest(targetUser.getId(), FollowStatus.FOLLOWING);

    // when
    followService.updateFollowStatus(request, currentUser.getId());
    FollowUpdateResponse response = followService.updateFollowStatus(request, currentUser.getId());

    // then
    assertThat(response.getFollowUserId()).isEqualTo(targetUser.getId());
    assertThat(response.getMessage()).isEqualTo("성공적으로 팔로우 처리했습니다.");
  }

  // ========== Helper Methods ==========

  private User createUser(String email, String nickname) {
    return User.builder()
        .email(email)
        .nickName(nickname)
        .imageUrl("https://example.com/image.jpg")
        .build();
  }
}
