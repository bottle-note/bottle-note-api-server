package app.bottlenote.accesscontrol.domain;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import app.bottlenote.common.domain.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Immutable;

/** IP 차단 append-only 감사 이력. */
@Getter
@Immutable
@Entity(name = "ipBanEvent")
@Table(name = "ip_ban_events")
@Comment("IP 차단 append-only 감사 이력")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpBanAuditRecord extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("IP 차단 현재 상태 ID")
  @Column(name = "ip_ban_id", nullable = false, updatable = false)
  private Long ipBanId;

  @Enumerated(EnumType.STRING)
  @Comment("이벤트 유형")
  @Column(name = "event_type", nullable = false, length = 20, updatable = false)
  private IpBanEventType eventType;

  @Comment("이벤트 사유")
  @Column(name = "reason", length = 200, updatable = false)
  private String reason;

  @Comment("변경 전 만료 시각")
  @Column(name = "previous_expires_at", updatable = false)
  private LocalDateTime previousExpiresAt;

  @Comment("변경 후 만료 시각")
  @Column(name = "next_expires_at", updatable = false)
  private LocalDateTime nextExpiresAt;

  @Enumerated(EnumType.STRING)
  @Comment("수행 주체 유형")
  @Column(name = "actor_type", nullable = false, length = 20, updatable = false)
  private IpBanActorType actorType;

  @Comment("인증된 관리자 ID")
  @Column(name = "actor_admin_user_id", updatable = false)
  private Long actorAdminUserId;

  @Comment("수행 Agent UUID")
  @Column(name = "actor_agent_id", columnDefinition = "char(36)", updatable = false)
  private String actorAgentId;

  @Comment("이벤트 발생 시각")
  @Column(name = "occurred_at", nullable = false, updatable = false)
  private LocalDateTime occurredAt;

  private IpBanAuditRecord(
      Long ipBanId,
      IpBanEventType eventType,
      String reason,
      LocalDateTime previousExpiresAt,
      LocalDateTime nextExpiresAt,
      IpBanActorType actorType,
      Long actorAdminUserId,
      String actorAgentId,
      LocalDateTime occurredAt) {
    this.ipBanId = Objects.requireNonNull(ipBanId, "ipBanId는 null일 수 없습니다.");
    this.eventType = Objects.requireNonNull(eventType, "eventType은 null일 수 없습니다.");
    this.reason = reason;
    this.previousExpiresAt = previousExpiresAt;
    this.nextExpiresAt = nextExpiresAt;
    this.actorType = Objects.requireNonNull(actorType, "actorType은 null일 수 없습니다.");
    this.actorAdminUserId = actorAdminUserId;
    this.actorAgentId = actorAgentId;
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt은 null일 수 없습니다.");
    validateActor();
  }

  public static IpBanAuditRecord create(
      Long ipBanId,
      IpBanEventType eventType,
      String reason,
      LocalDateTime previousExpiresAt,
      LocalDateTime nextExpiresAt,
      IpBanActorType actorType,
      Long actorAdminUserId,
      String actorAgentId,
      LocalDateTime occurredAt) {
    return new IpBanAuditRecord(
        ipBanId,
        eventType,
        reason,
        previousExpiresAt,
        nextExpiresAt,
        actorType,
        actorAdminUserId,
        actorAgentId,
        occurredAt);
  }

  private void validateActor() {
    switch (actorType) {
      case ADMIN -> {
        if (actorAdminUserId == null || actorAgentId != null) {
          throw new IllegalArgumentException("ADMIN 주체는 adminUserId만 가져야 합니다.");
        }
      }
      case AGENT -> {
        if (actorAdminUserId == null || actorAgentId == null || actorAgentId.isBlank()) {
          throw new IllegalArgumentException("AGENT 주체는 adminUserId와 agentId가 필요합니다.");
        }
      }
      case SYSTEM -> {
        if (actorAdminUserId != null || actorAgentId != null) {
          throw new IllegalArgumentException("SYSTEM 주체는 adminUserId와 agentId가 없어야 합니다.");
        }
      }
    }
  }
}
