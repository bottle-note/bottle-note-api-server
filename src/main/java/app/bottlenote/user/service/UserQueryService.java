package app.bottlenote.user.service;

import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

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


		// 예쁜 코드..?
		boolean isExistUser = !userQueryRepository.existsByUserId(userId);

		if (isExistUser) {
			throw new UserException(USER_NOT_FOUND);
		}


		// 쿼리 DSL
		// JpaUserQueryRepository 에서 쿼리 메소드를 사용하여 유저 정보를 조회한다.
		// CustomUserRepository
		// (c)CustomUSerRepositoryImpl 쿼리 메소드 필요 (유저 정보 조회)
		// (C)UserQuerySupporter 쿼리 서포터 필요 (찜하기 수 , 리뷰 수, 평가한 별점 수 , 팔로워 수 , 팔로우 수)
		return userQueryRepository.getMyPage(userId, currentUserId);

	}
}
