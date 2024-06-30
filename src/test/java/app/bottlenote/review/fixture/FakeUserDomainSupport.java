package app.bottlenote.review.fixture;

import app.bottlenote.user.service.domain.UserDomainSupport;

public class FakeUserDomainSupport implements UserDomainSupport {
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

	}
}
