package app.bottlenote.user.domain;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

	Notification save(Notification notification);

	Optional<Notification> findById(Long notifyId);

	List<Notification> findAll();
}
