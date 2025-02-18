package app.bottlenote.user.service;

import static app.bottlenote.user.domain.constant.UserStatus.ACTIVE;
import static app.bottlenote.user.dto.response.constant.WithdrawUserResultMessage.USER_WITHDRAW_SUCCESS;
import static app.bottlenote.user.exception.UserExceptionCode.MYBOTTLE_NOT_ACCESSIBLE;
import static app.bottlenote.user.exception.UserExceptionCode.MYPAGE_NOT_ACCESSIBLE;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserBasicService {

	private final UserRepository userRepository;
	private final UserFilterManager userFilterManager;

	@Transactional
	public NicknameChangeResponse nicknameChange(Long userId, NicknameChangeRequest request) {
		return userFilterManager.withActiveUserFilter(ACTIVE,
			() -> {
				String name = request.nickName();
				String beforeNickname;

				if (userRepository.existsByNickName(name)) {
					throw new UserException(UserExceptionCode.USER_NICKNAME_NOT_VALID);
				}

				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(USER_NOT_FOUND));

				beforeNickname = user.getNickName();

				user.changeNickName(name);
				Long updatedUser = user.getId();
				String newUserNickName = user.getNickName();

				return NicknameChangeResponse.builder()
					.message(NicknameChangeResponse.Message.SUCCESS)
					.userId(updatedUser)
					.beforeNickname(beforeNickname)
					.changedNickname(newUserNickName)
					.build();
			});
	}

	@Transactional
	public ProfileImageChangeResponse profileImageChange(Long userId, String viewUrl) {
		return userFilterManager.withActiveUserFilter(ACTIVE,
			() -> {
				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(USER_NOT_FOUND));

				user.changeProfileImage(viewUrl);

				return new ProfileImageChangeResponse(user.getId(), user.getImageUrl());
			});
	}

	@Transactional
	public WithdrawUserResultResponse withdrawUser(Long userId) {
		return userFilterManager.withActiveUserFilter(ACTIVE,
			() -> {
				User user = userRepository.findById(userId)
					.orElseThrow(() -> new UserException(USER_NOT_FOUND));

				user.withdrawUser();

				return WithdrawUserResultResponse.response(USER_WITHDRAW_SUCCESS, userId);
			});
	}

	/**
	 * 해당 사용자의 기본정보를 조회 합니다 (닉네임, 프로필 이미지, 소개글)
	 */
	@Transactional(readOnly = true)
	public MyPageResponse getMyPage(Long userId, Long currentUserId) {
		return userFilterManager.withActiveUserFilter(ACTIVE,
			() -> {
				boolean isUserNotAccessible = !userRepository.existsByUserId(userId);

				if (isUserNotAccessible) {
					throw new UserException(MYPAGE_NOT_ACCESSIBLE);
				}
				return userRepository.getMyPage(userId, currentUserId);
			});
	}

	/**
	 * 본인의 마이 보틀을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public MyBottleResponse getMyBottle(Long userId, Long currentUserId, MyBottleRequest myBottleRequest) {
		return userFilterManager.withActiveUserFilter(ACTIVE,
			() -> {
				boolean isUserNotAccessible = !userRepository.existsByUserId(userId);

				if (isUserNotAccessible) {
					throw new UserException(MYBOTTLE_NOT_ACCESSIBLE);
				}

				MyBottlePageableCriteria criteria = MyBottlePageableCriteria.of(
					myBottleRequest,
					userId,
					currentUserId
				);

				return userRepository.getMyBottle(criteria);
			});
	}
}
