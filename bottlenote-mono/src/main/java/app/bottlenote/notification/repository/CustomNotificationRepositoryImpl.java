package app.bottlenote.notification.repository;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** JPA id-desc keyset 조회. cursor&gt;0이면 id &lt; cursor, limit = pageSize + 1. */
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

  private final EntityManager entityManager;

  @Override
  public List<Notification> findPageByUserId(NotificationListCriteria criteria) {
    String jpql =
        criteria.hasCursor()
            ? """
              select n from notification n
              where n.userId = :userId and n.id < :cursor
              order by n.id desc
              """
            : """
              select n from notification n
              where n.userId = :userId
              order by n.id desc
              """;

    TypedQuery<Notification> query =
        entityManager
            .createQuery(jpql, Notification.class)
            .setParameter("userId", criteria.userId())
            .setMaxResults((int) criteria.fetchLimit());

    if (criteria.hasCursor()) {
      query.setParameter("cursor", criteria.cursor());
    }
    return query.getResultList();
  }

  @Override
  public long countByUserId(Long userId) {
    return entityManager
        .createQuery(
            """
            select count(n) from notification n
            where n.userId = :userId
            """,
            Long.class)
        .setParameter("userId", userId)
        .getSingleResult();
  }
}
