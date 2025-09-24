package app.external.notification.domain;

import app.bottlenote.core.common.domain.BaseEntity;
import app.external.notification.domain.constant.NotificationCategory;
import app.external.notification.domain.constant.NotificationStatus;
import app.external.notification.domain.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

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

  @Comment("알림의 타입( 시스템 알림, 사용자 알림, 프로모션 알림 )")
  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationType type;

  @Comment("알림의 종류 ( 리뷰, 댓글, 팔로우, 좋아요, 프로모션 )")
  @Column(name = "category", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationCategory category;

  @Comment("알림의 상태")
  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  @Comment("사용자 읽음 여부")
  @Column(name = "is_read", nullable = false)
  private Boolean isRead;

  public Notification() {}

  @Builder
  public Notification(
      Long id,
      Long userId,
      String title,
      String content,
      NotificationType type,
      NotificationCategory category,
      NotificationStatus status,
      Boolean isRead) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "사용자 식별자는 필수입니다.");
    this.title = Objects.requireNonNull(title, "알림 제목은 필수입니다.");
    this.content = Objects.requireNonNull(content, "알림 내용은 필수입니다.");
    this.type = type != null ? type : NotificationType.SYSTEM;
    this.category = Objects.requireNonNull(category, "알림 종류는 필수입니다.");
    this.status = status != null ? status : NotificationStatus.PENDING;
    this.isRead = isRead != null && isRead;
  }
}
