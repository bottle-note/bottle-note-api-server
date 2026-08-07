package app.external.notification.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.external.notification.domain.Notification;
import app.external.notification.domain.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaNotificationRepository
    extends NotificationRepository, JpaRepository<Notification, Long> {

  @Override
  @Query(
      """
			select n from notification n
			where n.id = :id and n.userId = :userId
			""")
  Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  @Override
  @Query(
      """
			select n from notification n
			where n.userId = :userId
			order by n.id desc
			""")
  List<Notification> findAllByUserIdOrderByIdDesc(@Param("userId") Long userId);

  @Override
  @Query(
      """
			select count(n) from notification n
			where n.userId = :userId and n.isRead = false
			""")
  long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

  @Override
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
			update notification n
			set n.isRead = true,
			    n.status = app.external.notification.domain.constant.NotificationStatus.READ
			where n.userId = :userId and n.isRead = false
			""")
  int markAllAsReadByUserId(@Param("userId") Long userId);
}
