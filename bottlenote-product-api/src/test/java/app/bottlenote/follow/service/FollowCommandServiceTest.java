package app.bottlenote.follow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bottlenote.global.pagination.CursorProperties;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.fixture.InMemoryFollowRepository;
import app.bottlenote.user.fixture.InMemoryUserQueryRepository;
import app.bottlenote.user.service.FollowService;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] FollowCommand")
class FollowCommandServiceTest {

  private FollowService followService;
  private InMemoryFollowRepository followRepository;
  private InMemoryUserQueryRepository userRepository;

  @BeforeEach
  void setUp() {
    followRepository = new InMemoryFollowRepository();
    userRepository = new InMemoryUserQueryRepository();
    var properties = new CursorProperties();
    properties.setCurrentKeyId("v1");
    properties.setCurrentSecret("test-pagination-cursor-secret");
    followService = new FollowService(followRepository, userRepository,
        new HmacCursorCodec(properties, Clock.systemUTC()), event -> {});
  }

  @Test
  @DisplayName("다른 유저를 팔로우 할 수 있다.")
  void test_1() {

    // given
    Long userId = 9L;
    Long followUserId = 1L;
    String email = "user@email";
    FollowUpdateRequest request = new FollowUpdateRequest(followUserId, FollowStatus.FOLLOWING);

    User followUser = User.builder().id(followUserId).email(email).nickName("userNickName").build();

    Follow follow =
        Follow.builder()
            .userId(userId)
            .targetUserId(followUserId)
            .status(FollowStatus.FOLLOWING)
            .build();

    followRepository.save(follow);
    userRepository.save(followUser);

    // when
    FollowUpdateResponse response = followService.updateFollowStatus(request, userId);

    // then
    assertEquals(followUserId, response.getFollowUserId());
    assertEquals(response.getNickName(), followUser.getNickName());
    assertEquals(response.getImageUrl(), followUser.getImageUrl());
    assertEquals(
        response.getMessage(), FollowUpdateResponse.Message.FOLLOW_SUCCESS.getResponseMessage());
  }

  @Test
  @DisplayName("유저를 언팔로우할 수 있다.")
  void test_2() {
    // given
    Long userId = 9L;
    Long followUserId = 1L;
    FollowUpdateRequest request = new FollowUpdateRequest(followUserId, FollowStatus.UNFOLLOW);

    User followUser =
        User.builder().id(followUserId).email("email").nickName("userNickName").build();

    Follow follow =
        Follow.builder()
            .userId(userId)
            .targetUserId(followUserId)
            .status(FollowStatus.FOLLOWING)
            .build();

    followRepository.save(follow);
    userRepository.save(followUser);

    // when
    FollowUpdateResponse response = followService.updateFollowStatus(request, userId);

    // then
    assertEquals(followUserId, response.getFollowUserId());
    assertEquals(response.getNickName(), followUser.getNickName());
    assertEquals(response.getImageUrl(), followUser.getImageUrl());
    assertEquals(
        response.getMessage(), FollowUpdateResponse.Message.UNFOLLOW_SUCCESS.getResponseMessage());
  }

  @Test
  @DisplayName("자기 자신을 팔로우할 수 없다.")
  void test_3() {
    // given
    Long userId = 9L;
    FollowUpdateRequest request = new FollowUpdateRequest(userId, FollowStatus.FOLLOWING);

    // when & then
    FollowException exception =
        assertThrows(
            FollowException.class, () -> followService.updateFollowStatus(request, userId));

    assertEquals(FollowExceptionCode.CANNOT_FOLLOW_SELF, exception.getExceptionCode());
  }
}
