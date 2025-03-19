package app.bottlenote.user.service;

import app.bottlenote.alcohols.dto.response.detail.FriendInfo;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.exception.FollowException;
import app.bottlenote.user.exception.FollowExceptionCode;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

		Follow follow = followRepository.findByUserIdAndFollowUserId(currentUserId, followUserId)
			.orElseGet(() -> {
				User user = userRepository.findById(currentUserId)
					.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

				return Follow.builder()
					.userId(user.getId())
					.targetUserId(request.followUserId())
					.build();
			});
		User targetUser = userRepository.findById(followUserId)
			.orElseThrow(() -> new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

		follow.updateStatus(request.status());
		followRepository.save(follow);

		return FollowUpdateResponse.builder()
			.status(follow.getStatus())
			.followUserId(followUserId)
			.nickName(targetUser.getNickName())
			.imageUrl(targetUser.getImageUrl())
			.build();
	}

	@Transactional(readOnly = true)
	public PageResponse<FollowingSearchResponse> getFollowingList(Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

		if (!userRepository.existsByUserId(currentUserId)) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}

		FollowPageableCriteria criteria = FollowPageableCriteria.of(
			pageableRequest.cursor(),
			pageableRequest.pageSize()
		);

		return followRepository.getFollowingList(userId, criteria);
	}

	@Transactional(readOnly = true)
	public PageResponse<FollowerSearchResponse> getFollowerList(Long currentUserId, Long userId, FollowPageableRequest pageableRequest) {

		if (!userRepository.existsByUserId(currentUserId)) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}

		FollowPageableCriteria criteria = FollowPageableCriteria.of(
			pageableRequest.cursor(),
			pageableRequest.pageSize()
		);

		return followRepository.getFollowerList(userId, criteria);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FriendInfo> getTastingFriendsInfoList(Long alcoholId, Long userId, PageRequest pageRequest) {
		return followRepository.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
	}
}
