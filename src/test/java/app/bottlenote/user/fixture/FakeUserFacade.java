package app.bottlenote.user.fixture;

import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.service.UserFacade;

public class FakeUserFacade implements UserFacade {
	@Override
	public Long countByUsername(String userName) {
		return 0L;
	}

	@Override
	public Boolean existsByUserId(Long userId) {
		return userId.equals(1L);
	}

	@Override
	public void isValidUserId(Long userId) {

	}

	@Override
	public UserProfileInfo getUserProfileInfo(Long userId) {
		return null;
	}
}
