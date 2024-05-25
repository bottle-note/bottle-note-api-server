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

		// TODO :: 팔로우 대상이 나를 차단 했을경우 팔로우 불가능처리

		Follow follow = followRepository.findByUserIdAndFollowUserIdWithFetch(userId, followUserId)
			.orElseGet(() -> {

				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
				User followUser = userRepository.findById(followUserId)
					.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

				return Follow.builder()
					.user(user)
					.followUser(followUser)
					.build();
			});

		String nickName = follow.getFollowUser().getNickName();
		String imageUrl = follow.getFollowUser().getImageUrl();

		follow.updateStatus(request.status());
		followRepository.save(follow);

		return FollowUpdateResponse.builder()
			.status(follow.getStatus())
			.followUserId(followUserId)
			.nickName(nickName)
			.imageUrl(imageUrl)
			.build();
	}
}
