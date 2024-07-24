package app.bottlenote.user.service;

import app.bottlenote.user.dto.response.MypageResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public MypageResponse searchMyPage(Long userId, Long currentUserId) {
		userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NICKNAME_NOT_VALID));

		boolean isMyPage = userId.equals(currentUserId);
		boolean isFollowing = currentUserId != null && userRepository.isFollowing(currentUserId, userId);

		MypageResponse mypageResponse = userRepository.getMypageData(userId, currentUserId);

		return MypageResponse.of(
			mypageResponse.userId(),
			mypageResponse.nickName(),
			mypageResponse.imageUrl(),
			mypageResponse.FollowingCount(),
			mypageResponse.FollowerCount(),
			mypageResponse.isPickCount(),
			mypageResponse.reviewCount(),
			mypageResponse.ratingCount(),
			isMyPage,
			isFollowing
		);
	}
}
