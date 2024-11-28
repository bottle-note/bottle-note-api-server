package app.bottlenote.user.service;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserFacade implements UserFacade {
	private final UserRepository userQueryRepository;

	@Override
	public Long countByUsername(String userName) {
		log.info("[domain] countByUsername : {}", userName);
		return userQueryRepository.countByUsername(userName);
	}

	@Override
	public Boolean existsByUserId(Long userId) {
		log.info("[domain] existsByUserId : {}", userId);
		return userQueryRepository.existsByUserId(userId);
	}

	@Override
	public void isValidUserId(Long userId) {
		log.info("[domain] isValidUserId : {}", userId);

		User user = userQueryRepository.findById(userId)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));

		log.info("[domain] isValidUserId success : {}", user.getId());
	}

	@Override
	public UserProfileInfo getUserProfileInfo(Long userId) {
		User user = userQueryRepository.findById(userId)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));

		return UserProfileInfo.create(user.getId(), user.getNickName(), user.getImageUrl());
	}
}
