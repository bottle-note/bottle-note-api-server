package app.bottlenote.follow.service;

import app.bottlenote.follow.domain.Follow;
import app.bottlenote.follow.dto.FollowUpdateRequest;
import app.bottlenote.follow.dto.FollowUpdateResponse;
import app.bottlenote.follow.exception.FollowException;
import app.bottlenote.follow.exception.FollowExceptionCode;
import app.bottlenote.follow.repository.FollowCommandRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FollowCommandService {

	private final UserCommandRepository userRepository;
	private final FollowCommandRepository followRepository;

	@Transactional
	public FollowUpdateResponse updateFollow(FollowUpdateRequest request, Long userId) {
		Long followUserId = request.followerUserId();
		boolean isFollow = request.isFollow();

		// Self-follow check
		if (userId.equals(followUserId)) {
			throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
		}

		// TODO :: 내가 차단 한 사용자인지 확인
		// TODO :: 내가 차단 당한 사용자인지 확인

		// User validation
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		User followUser = userRepository.findById(followUserId)
			.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

		FollowUpdateResponse.Message message;

		if (isFollow) {
			// Follow logic
			if (followRepository.findByUserIdAndFollowUserId(userId, followUserId).isPresent()) {
				throw new FollowException(FollowExceptionCode.ALREADY_FOLLOWING);
			}
			Follow follow = Follow.builder()
				.user(user)
				.followUser(followUser)
				.build();
			followRepository.save(follow);
			message = FollowUpdateResponse.Message.FOLLOW_SUCCESS;
		} else {
			// Unfollow logic
			Follow follow = followRepository.findByUserIdAndFollowUserId(userId, followUserId)
				.orElseThrow(() -> new FollowException(FollowExceptionCode.ALREADY_UNFOLLOWING));
			followRepository.delete(follow);
			message = FollowUpdateResponse.Message.UNFOLLOW_SUCCESS;
		}

		return FollowUpdateResponse.builder()
			.message(message)
			.followUserId(followUserId)
			.build();
	}
}
