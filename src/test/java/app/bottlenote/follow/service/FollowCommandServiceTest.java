package app.bottlenote.follow.service;

import app.bottlenote.follow.domain.Follow;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowUpdateResponse;
import app.bottlenote.follow.exception.FollowException;
import app.bottlenote.follow.exception.FollowExceptionCode;
import app.bottlenote.follow.repository.FollowCommandRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.repository.UserCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowCommandServiceTest {

	@InjectMocks
	FollowCommandService followCommandService;

	@Mock
	FollowCommandRepository followCommandRepository;

	@Mock
	UserCommandRepository userCommandRepository;

	@Test
	@DisplayName("다른 유저를 팔로우 할 수 있다.")
	void shouldFollowOtherUser() {

		// given
		Long userId = 9L;
		Long followUserId = 1L;
		String email = "user@email";
		FollowUpdateRequest request = new FollowUpdateRequest(followUserId, FollowStatus.FOLLOWING);

		User user = User.builder()
			.id(userId)
			.email(email)
			.nickName("userNickName").build();

		User followUser = User.builder()
			.id(followUserId)
			.email(email)
			.nickName("userNickName")
			.build();

		Follow follow = Follow.builder()
			.user(user)
			.followUser(followUser)
			.status(FollowStatus.FOLLOWING)
			.build();

		when(userCommandRepository.findById(followUserId)).thenReturn(Optional.of(user));
		when(userCommandRepository.findById(userId)).thenReturn(Optional.of(user));
		when(followCommandRepository.findByUserIdAndFollowUserIdWithFetch(userId, followUserId)).thenReturn(Optional.empty());
		when(followCommandRepository.save(any(Follow.class))).thenReturn(follow);

		// when
		FollowUpdateResponse response = followCommandService.updateFollowStatus(request, userId);

		// then
		assertEquals(response.getFollowUserId(), followUserId);
		assertEquals(response.getNickName(), followUser.getNickName());
		assertEquals(response.getImageUrl(), followUser.getImageUrl());
		assertEquals(response.getMessage(),FollowUpdateResponse.Message.FOLLOW_SUCCESS.getMessage());


	}

	@Test
	@DisplayName("유저를 언팔로우할 수 있다.")
	void shouldUnfollowUser() {
		// given
		Long userId = 9L;
		Long followUserId = 1L;
		FollowUpdateRequest request = new FollowUpdateRequest(followUserId, FollowStatus.UNFOLLOW);

		User user = User.builder()
			.id(userId)
			.email("email")
			.nickName("userNickName")
			.build();

		User followUser = User.builder()
			.id(followUserId)
			.email("email")
			.nickName("userNickName")
			.build();

		Follow follow = Follow.builder()
			.user(user)
			.followUser(followUser)
			.status(FollowStatus.FOLLOWING)
			.build();

		when(followCommandRepository.findByUserIdAndFollowUserIdWithFetch(userId, followUserId)).thenReturn(Optional.of(follow));
		when(followCommandRepository.save(any(Follow.class))).thenReturn(follow);

		// when
		FollowUpdateResponse response = followCommandService.updateFollowStatus(request, userId);

		// then
		assertEquals(response.getFollowUserId(), followUserId);
		assertEquals(response.getNickName(), followUser.getNickName());
		assertEquals(response.getImageUrl(), followUser.getImageUrl());
		assertEquals(response.getMessage(), FollowUpdateResponse.Message.UNFOLLOW_SUCCESS.getMessage());
	}

	@Test
	@DisplayName("자기 자신을 팔로우할 수 없다.")
	void shouldNotFollowSelf() {
		// given
		Long userId = 9L;
		FollowUpdateRequest request = new FollowUpdateRequest(userId, FollowStatus.FOLLOWING);

		// when & then
		FollowException exception = assertThrows(FollowException.class, () ->
			followCommandService.updateFollowStatus(request, userId)
		);

		assertEquals(FollowExceptionCode.CANNOT_FOLLOW_SELF, exception.getExceptionCode());
	}

}

