package app.bottlenote.follow.service;

import app.bottlenote.follow.domain.Follow;
import app.bottlenote.follow.dto.dsl.FollowPageableCriteria;
import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.dto.response.FollowUpdateResponse;
import app.bottlenote.follow.exception.FollowException;
import app.bottlenote.follow.exception.FollowExceptionCode;
import app.bottlenote.follow.repository.follow.FollowRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class FollowService {

	private final FollowRepository followRepository;
	private final UserCommandRepository userRepository;

	@Transactional
	public FollowUpdateResponse updateFollowStatus(FollowUpdateRequest request, Long userId) {
		Long followUserId = request.followUserId();

		if (userId.equals(followUserId)) {
			throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
		}

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

	@Transactional(readOnly = true)
	public PageResponse<FollowSearchResponse> findFollowList(Long userId, FollowPageableRequest pageableRequest) {

		FollowPageableCriteria criteria = FollowPageableCriteria.of(
			pageableRequest.cursor(),
			pageableRequest.pageSize(),
			userId
		);

		return followRepository.followList(criteria);
	}

}
