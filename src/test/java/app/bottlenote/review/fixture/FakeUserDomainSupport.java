package app.bottlenote.review.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.domain.UserDomainSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FakeUserDomainSupport implements UserDomainSupport {
	private static final Logger log = LogManager.getLogger(FakeUserDomainSupport.class);
	Map<Long, User> users = new HashMap<>();

	public FakeUserDomainSupport(User... users) {
		for (User user : users) {
			log.info("[Fake] UserDomainSupport : user = {}", user);
			this.users.put(user.getId(), user);
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
		User user = users.get(userId);
		if (user == null) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
	}
}
