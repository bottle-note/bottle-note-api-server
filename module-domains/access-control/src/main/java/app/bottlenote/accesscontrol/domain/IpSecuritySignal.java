package app.bottlenote.accesscontrol.domain;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
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
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** IP 보안 탐지 근거와 관리자의 판정 메타데이터. */
@Getter
@Entity(name = "ipSecuritySignal")
@Table(name = "ip_security_signals")
@Comment("IP 보안 탐지 근거와 판정 메타데이터")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IpSecuritySignal extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ip_ban_id")
  private Long ipBanId;

  @Column(name = "normalized_ip", nullable = false, length = 45)
  private String normalizedIp;

  @Column(name = "endpoint_path", nullable = false, length = 1024)
  private String endpointPath;

  @Column(name = "http_method", nullable = false, length = 10)
  private String httpMethod;

  @Column(name = "rule_code", nullable = false, length = 100)
  private String ruleCode;

  @Column(name = "observed_from", nullable = false)
  private LocalDateTime observedFrom;

  @Column(name = "observed_until", nullable = false)
  private LocalDateTime observedUntil;

  @Column(name = "observation_count", nullable = false)
  private int observationCount;

  @Column(name = "reported_by_admin_user_id")
  private Long reportedByAdminUserId;

  @Column(name = "reported_by_agent_id", columnDefinition = "char(36)")
  private String reportedByAgentId;

  @Column(name = "agent_version", length = 100)
  private String agentVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "verdict", nullable = false, length = 30)
  private SignalVerdict verdict;

  @Column(name = "reviewed_by_admin_user_id")
  private Long reviewedByAdminUserId;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "review_note", length = 500)
  private String reviewNote;

  private IpSecuritySignal(
      Long ipBanId,
      String normalizedIp,
      String endpointPath,
      String httpMethod,
      String ruleCode,
      LocalDateTime observedFrom,
      LocalDateTime observedUntil,
      int observationCount,
      Long reportedByAdminUserId,
      String reportedByAgentId,
      String agentVersion) {
    this.ipBanId = ipBanId;
    this.normalizedIp = Objects.requireNonNull(normalizedIp, "normalizedIp는 null일 수 없습니다.");
    this.endpointPath = requireEndpoint(endpointPath);
    this.httpMethod = requireText(httpMethod, 10, "httpMethod").toUpperCase(Locale.ROOT);
    this.ruleCode = requireText(ruleCode, 100, "ruleCode");
    this.observedFrom = Objects.requireNonNull(observedFrom, "observedFrom은 null일 수 없습니다.");
    this.observedUntil = Objects.requireNonNull(observedUntil, "observedUntil은 null일 수 없습니다.");
    this.observationCount = observationCount;
    this.reportedByAdminUserId = reportedByAdminUserId;
    this.reportedByAgentId = blankToNull(reportedByAgentId);
    this.agentVersion = blankToNull(agentVersion);
    this.verdict = SignalVerdict.UNKNOWN;
    validateObservation();
    validateAgentMetadata();
  }

  public static IpSecuritySignal report(
      Long ipBanId,
      String normalizedIp,
      String endpointPath,
      String httpMethod,
      String ruleCode,
      LocalDateTime observedFrom,
      LocalDateTime observedUntil,
      int observationCount,
      Long reportedByAdminUserId,
      String reportedByAgentId,
      String agentVersion) {
    return new IpSecuritySignal(
        ipBanId,
        normalizedIp,
        endpointPath,
        httpMethod,
        ruleCode,
        observedFrom,
        observedUntil,
        observationCount,
        reportedByAdminUserId,
        reportedByAgentId,
        agentVersion);
  }

  public void review(
      SignalVerdict verdict,
      Long reviewedByAdminUserId,
      LocalDateTime reviewedAt,
      String reviewNote) {
    if (this.verdict != SignalVerdict.UNKNOWN) {
      throw new IllegalStateException("UNKNOWN 상태의 signal만 판정할 수 있습니다.");
    }
    if (verdict == null || verdict == SignalVerdict.UNKNOWN || reviewedByAdminUserId == null) {
      throw new IllegalArgumentException("확정 판정과 검토 관리자 ID가 필요합니다.");
    }
    this.verdict = verdict;
    this.reviewedByAdminUserId = reviewedByAdminUserId;
    this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt은 null일 수 없습니다.");
    this.reviewNote = reviewNote == null ? null : requireText(reviewNote, 500, "reviewNote");
  }

  private void validateObservation() {
    if (observedUntil.isBefore(observedFrom) || observationCount < 1) {
      throw new IllegalArgumentException("관찰 구간과 횟수가 유효하지 않습니다.");
    }
  }

  private void validateAgentMetadata() {
    if (reportedByAgentId != null && reportedByAdminUserId == null) {
      throw new IllegalArgumentException("Agent reporter에는 관리자 ID가 필요합니다.");
    }
    if (reportedByAgentId == null && agentVersion != null) {
      throw new IllegalArgumentException("agentVersion에는 Agent ID가 필요합니다.");
    }
    if (reportedByAgentId != null && agentVersion == null) {
      throw new IllegalArgumentException("Agent reporter에는 agentVersion이 필요합니다.");
    }
  }

  private static String requireEndpoint(String endpointPath) {
    String value = requireText(endpointPath, 1024, "endpointPath");
    if (value.contains("?")) {
      throw new IllegalArgumentException("endpointPath에는 query string을 포함할 수 없습니다.");
    }
    return value;
  }

  private static String requireText(String value, int maxLength, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + "은 비어 있을 수 없습니다.");
    }
    String trimmed = value.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException(name + "은 최대 " + maxLength + "자입니다.");
    }
    return trimmed;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
