package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserFacade;

import java.util.List;

public class FakeUserFacade implements UserFacade {

	private List<Long> userDatabase = List.of(1L, 2L, 3L);

	@Override
	public Boolean existsByUserId(Long userId) {
		return userDatabase.contains(userId);
	}

	@Override
	public void isValidUserId(Long userId) {
		if (existsByUserId(userId)) {
			throw new UserException(UserExceptionCode.USER_NOT_FOUND);
		}
	}

	@Override
	public UserProfileInfo getUserProfileInfo(Long userId) {
		return null;
	}
}
