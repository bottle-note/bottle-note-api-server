package app.bottlenote.notification.repository;

import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.notification.domain.Notification;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;

/** JPA id-desc keyset 조회. cursor&gt;0이면 id &lt; cursor, limit = pageSize + 1. */
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

  private final EntityManager entityManager;
  private final AuditorAware<AuditPrincipal> auditorAware;

  @Override
  public void saveIfAbsent(Notification notification) {
    AuditPrincipal principal = auditorAware.getCurrentAuditor().orElse(null);
    LocalDateTime now = LocalDateTime.now();
    // 중복일 때 원본 본문과 읽음 상태를 보존하며 경쟁 요청도 정상 종료한다.
    entityManager
        .createNativeQuery(
            """
        INSERT INTO notifications
          (user_id, title, content, type, category, status, is_read, read_at,
           source_type, source_id, action_type, action_target_id, action_payload, action_version,
           create_at, last_modify_at, create_principal_id, create_principal_type, create_principal_email,
           last_modify_principal_id, last_modify_principal_type, last_modify_principal_email)
        VALUES
          (:userId, :title, :content, :type, :category, :status, :isRead, :readAt,
           :sourceType, :sourceId, :actionType, :actionTargetId, :actionPayload, :actionVersion,
           :createdAt, :createdAt, :principalId, :principalType, :principalEmail,
           :principalId, :principalType, :principalEmail)
        ON DUPLICATE KEY UPDATE id = id
        """)
        .setParameter("userId", notification.getUserId())
        .setParameter("title", notification.getTitle())
        .setParameter("content", notification.getContent())
        .setParameter("type", notification.getType().name())
        .setParameter("category", notification.getCategory().name())
        .setParameter("status", notification.getStatus().name())
        .setParameter("isRead", notification.getIsRead())
        .setParameter("readAt", notification.getReadAt())
        .setParameter("sourceType", notification.getSourceType())
        .setParameter("sourceId", notification.getSourceId())
        .setParameter("actionType", notification.getActionType())
        .setParameter("actionTargetId", notification.getActionTargetId())
        .setParameter(
            "actionPayload",
            notification.getActionPayload() == null
                ? null
                : notification.getActionPayload().toString())
        .setParameter("actionVersion", notification.getActionVersion())
        .setParameter("createdAt", now)
        .setParameter("principalId", principal == null ? null : principal.getId())
        .setParameter(
            "principalType",
            principal == null || principal.getType() == null ? null : principal.getType().name())
        .setParameter("principalEmail", principal == null ? null : principal.getEmail())
        .executeUpdate();
  }

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
