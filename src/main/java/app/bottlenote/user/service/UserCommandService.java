package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCommandService {

	private final UserCommandRepository userCommandRepository;

	/**
	 * 닉네임 변경
	 *
	 * @param userId  the user id
	 * @param request the request
	 * @return the nickname change response
	 */
	@Transactional
	public NicknameChangeResponse nicknameChange(Long userId, NicknameChangeRequest request) {

		log.info("userId : {}", userId);
		log.info("request : {}", request);

		String name = request.nickName();
		String beforeNickname;

		if (userCommandRepository.existsByNickName(name)) {
			throw new UserException(UserExceptionCode.USER_NICKNAME_NOT_VALID);
		}

		User user = userCommandRepository.findById(userId)
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
	}

	/**
	 * 프로필 이미지 변경
	 *
	 * @param userId  the user id
	 * @param viewUrl the view url
	 * @return the profile image change response
	 */
	@Transactional
	public ProfileImageChangeResponse profileImageChange(Long userId, String viewUrl) {

		User user = userCommandRepository.findById(userId)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));

		user.changeProfileImage(viewUrl);

		return ProfileImageChangeResponse.builder()
			.userId(user.getId())
			.profileImageUrl(user.getImageUrl())
			.build();
	}
}
