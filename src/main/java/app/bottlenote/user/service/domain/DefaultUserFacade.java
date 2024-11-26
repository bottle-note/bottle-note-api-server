package app.bottlenote.user.service.domain;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

@Service
public class DefaultUserFacade implements UserFacade {
	private static final Logger log = LogManager.getLogger(DefaultUserFacade.class);
	private final UserQueryRepository userQueryRepository;

	public DefaultUserFacade(UserQueryRepository userQueryRepository) {
		this.userQueryRepository = userQueryRepository;
	}

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
