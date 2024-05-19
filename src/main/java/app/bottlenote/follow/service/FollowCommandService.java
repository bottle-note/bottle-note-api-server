package app.bottlenote.follow.service;

import app.bottlenote.follow.domain.Follow;
import app.bottlenote.follow.domain.constant.FollowStatus;
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
		FollowStatus followStatus = request.isFollow();

		if (userId.equals(followUserId)) {
			throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		User followUser = userRepository.findById(followUserId)
			.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

		Follow followEntity = followRepository.findByFollowUser(userId, followUserId)
			.orElseGet(() -> Follow.builder()
				.user(user)
				.followUser(followUser)
				.status(FollowStatus.UNFOLLOW) // 기본 상태를 UNFOLLOW로 설정
				.build());

		followEntity.updateFollowStatus(followStatus);
		followRepository.save(followEntity);

		String message = (followStatus == FollowStatus.FOLLOWING)
			? FollowUpdateResponse.Message.FOLLOW_SUCCESS.getMessage()
			: FollowUpdateResponse.Message.UNFOLLOW_SUCCESS.getMessage();

		return FollowUpdateResponse.builder()
			.message(message)
			.followUserId(followUserId)
			.build();
	}
}
