package app.bottlenote.review.fixture;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FakeUserDomainSupport implements UserFacade {
	Map<Long, User> dataSource = new HashMap<>();

	public FakeUserDomainSupport(User... users) {
		for (User user : users) {
			Long userId = user.getId() == null ? dataSource.size() + 1 : user.getId();
			ReflectionTestUtils.setField(user, "id", userId);
			this.dataSource.put(userId, user);
		}
	}

	@Override
	public Boolean existsByUserId(Long userId) {
		return null;
	}

	@Override
	public void isValidUserId(Long userId) {
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
