package app.bottlenote.user.service;

import app.bottlenote.user.domain.constant.UserStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFilterManager {

	private final EntityManager entityManager;

	/**
	 * 필터를 활성화하고 작업을 수행한 후 이후에 필터를 비활성화
	 */
	public <T> T withActiveUserFilter(UserStatus status, java.util.function.Supplier<T> action) {
		enableStatusFilter(status);
		try {
			return action.get();
		} finally {
			disableStatusFilter();
		}
	}

	/**
	 * 특정 상태의 유저만 조회하도록 필터를 활성화
	 */
	private void enableStatusFilter(UserStatus status) {
		Session session = entityManager.unwrap(Session.class);
		Filter filter = session.enableFilter("statusFilter");
		filter.setParameter("userStatus", status.name());
	}

	/**
	 * 필터를 비활성화해서 모든 유저 조회 가능
	 */
	private void disableStatusFilter() {
		Session session = entityManager.unwrap(Session.class);
		session.disableFilter("statusFilter");
	}
}