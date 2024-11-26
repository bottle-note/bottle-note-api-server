package app.bottlenote.review.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.domain.UserFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

public class FakeUserDomainSupport implements UserFacade {
	private static final Logger log = LogManager.getLogger(FakeUserDomainSupport.class);
	Map<Long, User> dataSource = new HashMap<>();

	public FakeUserDomainSupport(User... users) {
		for (User user : users) {
			Long userId = user.getId() == null ? dataSource.size() + 1 : user.getId();
			ReflectionTestUtils.setField(user, "id", userId);
			this.dataSource.put(userId, user);
			log.info("[Fake] init UserDomainSupport : user = {}", user);
		}
	}

	@Override
	public Long countByUsername(String userName) {
		return 0L;
	}

	@Override
	public Boolean existsByUserId(Long userId) {
		return null;
	}

	@Override
	public void isValidUserId(Long userId) {
		log.info("[Fake] isValidUserId : {}", userId);
		User user = dataSource.get(userId);
		if (user == null) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
	}

	@Override
	public UserProfileInfo getUserProfileInfo(Long userId) {
		User user = dataSource.get(userId);
		return UserProfileInfo.create(user.getId(), user.getNickName(), user.getImageUrl());
	}
}
