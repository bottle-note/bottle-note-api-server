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
	public FollowUpdateResponse updateFollowStatus(FollowUpdateRequest request, Long userId) {
		Long followUserId = request.followerUserId();

		if (userId.equals(followUserId)) {
			throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
		}

		Follow follow = followRepository.findByUserIdAndFollowUserId(userId, followUserId)
			.orElseGet(() -> {

				// 유저와 팔로우유저의 관계가 아무것도 없을때, 언팔로우가 오는것을 방지
				if (request.status() == FollowStatus.UNFOLLOW) {
					throw new FollowException(FollowExceptionCode.CANNOT_UNFOLLOW);
				}

				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
				User followUser = userRepository.findById(followUserId)
					.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

				return Follow.builder()
					.user(user)
					.followUser(followUser)
					.status(FollowStatus.FOLLOWING)
					.build();
			});

		follow.updateStatus(request.status());
		followRepository.save(follow);

		return FollowUpdateResponse.builder()
			.message(request.status() == FollowStatus.FOLLOWING ?
				FollowUpdateResponse.Message.FOLLOW_SUCCESS :
				FollowUpdateResponse.Message.UNFOLLOW_SUCCESS)
			.followUserId(followUserId)
			.build();
	}
}
