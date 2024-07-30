package app.bottlenote.user.service;

import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.user.exception.UserExceptionCode.MYPAGE_NOT_ACCESSIBLE;

@RequiredArgsConstructor
@Service
public class UserQueryService {

	private final UserQueryRepository userQueryRepository;

	/**
	 * 마이페이지 조회
	 *
	 * @param userId        마이페이지 조회 대상 사용자
	 * @param currentUserId 로그인 사용자
	 * @return the mypage
	 */
	@Transactional(readOnly = true)
	public MyPageResponse getMypage(Long userId, Long currentUserId) {

		boolean isUserNotAccessible = !userQueryRepository.existsByUserId(userId);

		if (isUserNotAccessible) {
			throw new UserException(MYPAGE_NOT_ACCESSIBLE);
		}

		return userQueryRepository.getMyPage(userId, currentUserId);

	}
}
