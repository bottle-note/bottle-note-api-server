package app.bottlenote.user.repository;

import app.bottlenote.user.domain.Notification;
import app.bottlenote.user.domain.NotificationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNotificationRepository extends NotificationRepository, JpaRepository<Notification, Long> {
}
