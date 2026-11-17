package app.bottlenote.accesscontrol.domain;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
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

/** IP별 차단 현재 상태. 정규화 IP당 1행을 유지한다. */
@Getter
@Entity(name = "ipBan")
@Table(name = "ip_bans")
@Comment("IP별 차단 현재 상태")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpBan extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("정규화된 IPv4 또는 IPv6 문자열")
  @Column(name = "normalized_ip", nullable = false, length = 45, unique = true)
  private String normalizedIp;

  @Enumerated(EnumType.STRING)
  @Comment("현재 상태")
  @Column(name = "status", nullable = false, length = 20)
  private IpBanStatus status;

  @Comment("차단 또는 변경 사유")
  @Column(name = "reason", nullable = false, length = 200)
  private String reason;

  @Comment("현재 차단 효력 시작 시각")
  @Column(name = "effective_from", nullable = false)
  private LocalDateTime effectiveFrom;

  @Comment("현재 차단 만료 시각")
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Comment("현재 상태 변경 시각")
  @Column(name = "state_changed_at", nullable = false)
  private LocalDateTime stateChangedAt;

  private IpBan(
      String normalizedIp,
      String reason,
      LocalDateTime effectiveFrom,
      LocalDateTime expiresAt,
      LocalDateTime stateChangedAt) {
    this.normalizedIp = Objects.requireNonNull(normalizedIp, "normalizedIp는 null일 수 없습니다.");
    this.status = IpBanStatus.ACTIVE;
    this.reason = Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "effectiveFrom은 null일 수 없습니다.");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt은 null일 수 없습니다.");
    this.stateChangedAt = Objects.requireNonNull(stateChangedAt, "stateChangedAt은 null일 수 없습니다.");
    requireValidExpiry();
  }

  public static IpBan createActive(
      String normalizedIp,
      String reason,
      LocalDateTime effectiveFrom,
      LocalDateTime expiresAt,
      LocalDateTime stateChangedAt) {
    return new IpBan(normalizedIp, reason, effectiveFrom, expiresAt, stateChangedAt);
  }

  /** 최초 차단 또는 종료 후 재차단. */
  public void ban(String reason, LocalDateTime now, LocalDateTime expiresAt) {
    this.status = IpBanStatus.ACTIVE;
    this.reason = Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    this.effectiveFrom = Objects.requireNonNull(now, "now는 null일 수 없습니다.");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt은 null일 수 없습니다.");
    this.stateChangedAt = now;
    requireValidExpiry();
  }

  /** 활성 차단의 TTL·사유 연장/갱신. */
  public void extend(String reason, LocalDateTime now, LocalDateTime expiresAt) {
    if (this.status != IpBanStatus.ACTIVE) {
      throw new IllegalStateException("ACTIVE 상태에서만 연장할 수 있습니다.");
    }
    this.reason = Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt은 null일 수 없습니다.");
    this.stateChangedAt = Objects.requireNonNull(now, "now는 null일 수 없습니다.");
    requireValidExpiry();
  }

  public void unban(String reason, LocalDateTime now) {
    if (this.status != IpBanStatus.ACTIVE) {
      throw new IllegalStateException("ACTIVE 상태에서만 해제할 수 있습니다.");
    }
    this.status = IpBanStatus.UNBANNED;
    this.reason = Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    this.stateChangedAt = Objects.requireNonNull(now, "now는 null일 수 없습니다.");
  }

  public void expire(String reason, LocalDateTime now) {
    if (this.status != IpBanStatus.ACTIVE) {
      throw new IllegalStateException("ACTIVE 상태에서만 만료 처리할 수 있습니다.");
    }
    this.status = IpBanStatus.EXPIRED;
    this.reason = Objects.requireNonNull(reason, "reason은 null일 수 없습니다.");
    this.stateChangedAt = Objects.requireNonNull(now, "now는 null일 수 없습니다.");
  }

  public boolean isActive() {
    return status == IpBanStatus.ACTIVE;
  }

  private void requireValidExpiry() {
    if (!expiresAt.isAfter(effectiveFrom)) {
      throw new IllegalArgumentException("expiresAt은 effectiveFrom보다 이후여야 합니다.");
    }
  }
}
