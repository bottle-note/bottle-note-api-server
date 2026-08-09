package app.bottlenote.notification.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.domain.NotificationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaNotificationRepository
    extends NotificationRepository,
        JpaRepository<Notification, Long>,
        CustomNotificationRepository {

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
			select count(n) from notification n
			where n.userId = :userId and n.isRead = false
			""")
  long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

  @Override
  boolean existsBySourceTypeAndSourceIdAndUserId(
      String sourceType, Long sourceId, Long userId);

  @Override
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
			update notification n
			set n.isRead = true,
			    n.readAt = coalesce(n.readAt, :readAt)
			where n.id = :id and n.userId = :userId and n.isRead = false
			""")
  int markAsReadByIdAndUserId(
      @Param("id") Long id,
      @Param("userId") Long userId,
      @Param("readAt") LocalDateTime readAt);

  @Override
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
			update notification n
			set n.isRead = true,
			    n.readAt = coalesce(n.readAt, :readAt)
			where n.userId = :userId and n.isRead = false
			""")
  int markAllAsReadByUserId(
      @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
