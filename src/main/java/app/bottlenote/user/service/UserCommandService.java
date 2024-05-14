package app.bottlenote.user.service;

import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.bottlenote.user.domain.User;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCommandService {

	private final UserCommandRepository userCommandRepository;

	@Transactional
	public NicknameChangeResponse nicknameChange(NicknameChangeRequest request) {

		if (request.nickName() == null) {
			throw new UserException(UserExceptionCode.NOT_FOUND_NICKNAME);
		}

		User user = userCommandRepository.findById(request.userId())
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		if (isExistNickname(request.nickName())) {
			throw new UserException(UserExceptionCode.USER_ALREADY_EXISTS);
		}

		String beforeNickname = user.getNickName();
		user.changeNickName(request.nickName());

		return NicknameChangeResponse.of(
			NicknameChangeResponse.Message.SUCCESS,
			user.getId(),
			beforeNickname,
			user.getNickName()
		);
	}

	// 추 후 비속어 필터링 로직 추가 하면서 메소드명도 ValidateNickname으로 변경
	private boolean isExistNickname(String nickname) {
		return userCommandRepository.existsByNickName(nickname);
	}
}
