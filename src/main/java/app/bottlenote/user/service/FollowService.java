package app.bottlenote.user.service;

import app.bottlenote.alcohols.dto.response.detail.FriendInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowSearchResponse;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.FollowRepository;
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

	@Transactional
	public FollowUpdateResponse updateFollowStatus(FollowUpdateRequest request, Long currentUserId) {
		Long followUserId = request.followUserId();

		if (currentUserId.equals(followUserId)) {
			throw new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF);
		}

		Follow follow = followRepository.findByUserIdAndFollowUserIdWithFetch(currentUserId, followUserId)
			.orElseGet(() -> {
				User user = userRepository.findById(currentUserId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

				User followUser = userRepository.findById(followUserId)
					.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

				return Follow.builder()
					.userId(user.getId())
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
	public PageResponse<FollowSearchResponse> getRelationList(Long userId, FollowPageableRequest pageableRequest) {

		FollowPageableCriteria criteria = FollowPageableCriteria.of(
			pageableRequest.cursor(),
			pageableRequest.pageSize()
		);

		return followRepository.getRelationList(userId, criteria);
	}

	@Override
	public List<FriendInfo> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest) {
		return followRepository.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
	}
}
