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

	// 추 후 비속어 필터링 로직 추가 하면서 메소드명도 ValidateNickname으로 변경
	private boolean isExistNickname(String nickname) {
		return userCommandRepository.existsByNickName(nickname);
	}
}
