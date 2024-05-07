package app.bottlenote.user.service;

import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.bottlenote.user.domain.User;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCommandService {

	private final UserCommandRepository userCommandRepository;

	public boolean isExistNickname(String nickname) {
		return userCommandRepository.existsByNickName(nickname);
	}

	@Transactional
	public String nicknameChange(NicknameChangeRequest nicknameChangeRequest) {
		User user = userCommandRepository.findById(nicknameChangeRequest.userId())
			.orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다."));
		user.changeNickName(nicknameChangeRequest.nickName());  // 닉네임을 업데이트하기 위해 세터 사용
		return user.getNickName();
	}
}
