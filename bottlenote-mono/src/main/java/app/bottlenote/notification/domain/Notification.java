package app.bottlenote.notification.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.notification.action.NotificationAction;
import app.bottlenote.notification.constant.NotificationEventAction;
import app.bottlenote.notification.constant.NotificationSettingGroup;
import app.bottlenote.notification.constant.NotificationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

@Getter
@Table(name = "notifications")
@Entity(name = "notification")
public class Notification extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("알림 대상 사용자 식별자")
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Comment("알림 제목")
  @Column(name = "title", nullable = false)
  private String title;

  @Comment("알림 내용")
  @Column(name = "content", nullable = false)
  private String content;

  @Comment("알림의 상태")
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  @Comment("사용자 읽음 여부")
  @Column(name = "is_read", nullable = false)
  private Boolean isRead;

  @Comment("사용자가 최초로 알림을 읽은 시각")
  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Comment("표준 알림 발생 액션. 식별하지 못한 과거 알림은 NULL")
  @Enumerated(EnumType.STRING)
  @Column(name = "event_action", length = 64)
  private NotificationEventAction eventAction;

  @Comment("알림 원본 유형")
  @Column(name = "source_type")
  private String sourceType;

  @Comment("알림 원본 ID")
  @Column(name = "source_id")
  private Long sourceId;

  @Comment("알림 Action 유형")
  @Column(name = "action_type")
  private String actionType;

  @Comment("알림 Action 대상 ID")
  @Column(name = "action_target_id")
  private Long actionTargetId;

  @Comment("알림 Action 부가 데이터")
  @Column(name = "action_payload", columnDefinition = "json")
  @Type(JsonType.class)
  private JsonNode actionPayload;

  @Comment("알림 Action 스키마 버전")
  @Column(name = "action_version")
  private Short actionVersion;

  protected Notification() {}

  @Builder
  public Notification(
      Long id,
      Long userId,
      String title,
      String content,
      NotificationStatus status,
      Boolean isRead,
      LocalDateTime readAt,
      NotificationEventAction eventAction,
      String sourceType,
      Long sourceId,
      NotificationAction action) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "사용자 식별자는 필수입니다.");
    this.title = Objects.requireNonNull(title, "알림 제목은 필수입니다.");
    this.content = Objects.requireNonNull(content, "알림 내용은 필수입니다.");
    this.status = status != null ? status : NotificationStatus.PENDING;
    this.isRead = isRead != null && isRead;
    this.readAt = readAt;
    this.eventAction = eventAction;
    this.sourceType = sourceType;
    this.sourceId = sourceId;
    if (action != null) {
      this.actionType = action.type().name();
      this.actionTargetId = action.targetId();
      this.actionPayload = action.payload();
      this.actionVersion = action.version().shortValue();
    }
  }

  public NotificationSettingGroup getGroup() {
    return eventAction == null ? null : eventAction.getGroup();
  }

  /** 알림을 읽음 처리하고 최초 읽음 시각을 보존한다. */
  public void markAsRead() {
    markAsRead(LocalDateTime.now());
  }

  /** 지정한 시각으로 알림을 읽음 처리하고 최초 읽음 시각을 보존한다. */
  public void markAsRead(LocalDateTime now) {
    Objects.requireNonNull(now, "읽음 시각은 필수입니다.");
    if (Boolean.TRUE.equals(this.isRead)) {
      return;
    }
    this.isRead = true;
    this.readAt = this.readAt != null ? this.readAt : now;
  }
}
