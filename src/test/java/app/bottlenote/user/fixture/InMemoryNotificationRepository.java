package app.bottlenote.user.fixture;

import app.bottlenote.user.domain.Notification;
import app.bottlenote.user.domain.NotificationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryNotificationRepository implements NotificationRepository {

	private final Map<Long, Notification> dataSource = new HashMap<>();


	@Override
	public Notification save(Notification notification) {
		long id = dataSource.size() + 1L;
		dataSource.put(id, notification);
		ReflectionTestUtils.setField(notification, "id", id);
		return notification;
	}

	@Override
	public Optional<Notification> findById(Long notifyId) {
		return dataSource.values().stream()
			.filter(notification -> notification.getId().equals(notifyId))
			.findFirst();
	}

	@Override
	public List<Notification> findAll() {
		return List.copyOf(dataSource.values());
	}
}
