package app.bottlenote.notification.repository;

import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/** JPA id-desc keyset 조회. cursor&gt;0이면 id &lt; cursor, limit = pageSize + 1. */
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

  private final EntityManager entityManager;

  @Override
  public List<Notification> findPageByUserId(NotificationListCriteria criteria) {
    List<String> conditions = filterConditions(criteria);
    if (criteria.hasCursor()) {
      conditions.add("n.id < :cursor");
    }
    String jpql =
        "select n from notification n where "
            + String.join(" and ", conditions)
            + " order by n.id desc";

    TypedQuery<Notification> query =
        entityManager
            .createQuery(jpql, Notification.class)
            .setMaxResults((int) criteria.fetchLimit());
    bindFilterParameters(query, criteria);
    if (criteria.hasCursor()) {
      query.setParameter("cursor", criteria.cursor());
    }
    return query.getResultList();
  }

  @Override
  public long countByCriteria(NotificationListCriteria criteria) {
    String jpql =
        "select count(n) from notification n where "
            + String.join(" and ", filterConditions(criteria));
    TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
    bindFilterParameters(query, criteria);
    return query.getSingleResult();
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

  private List<String> filterConditions(NotificationListCriteria criteria) {
    List<String> conditions = new ArrayList<>();
    conditions.add("n.userId = :userId");
    if (!criteria.types().isEmpty()) {
      conditions.add("n.type in :types");
    }
    if (!criteria.categories().isEmpty()) {
      conditions.add("n.category in :categories");
    }
    switch (criteria.readStatus()) {
      case READ -> conditions.add("n.isRead = true");
      case UNREAD -> conditions.add("n.isRead = false");
      case ALL -> {
        // 조건 없음
      }
    }
    if (criteria.createdFrom() != null) {
      conditions.add("n.createAt >= :createdFrom");
    }
    if (criteria.createdTo() != null) {
      conditions.add("n.createAt < :createdTo");
    }
    return conditions;
  }

  private void bindFilterParameters(Query query, NotificationListCriteria criteria) {
    query.setParameter("userId", criteria.userId());
    if (!criteria.types().isEmpty()) {
      query.setParameter("types", criteria.types());
    }
    if (!criteria.categories().isEmpty()) {
      query.setParameter("categories", criteria.categories());
    }
    if (criteria.createdFrom() != null) {
      query.setParameter("createdFrom", criteria.createdFrom());
    }
    if (criteria.createdTo() != null) {
      query.setParameter("createdTo", criteria.createdTo());
    }
  }
}
