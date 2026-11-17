package app.bottlenote.mfds.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity(name = "mfds_matching_selection")
@Table(name = "mfds_matching_selections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfdsMatchingSelection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id")
  private Long runId;

  @Column(name = "declaration_id", nullable = false)
  private Long declarationId;

  @Column(name = "target_type", nullable = false, length = 16)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(nullable = false, length = 16)
  private String action;

  @Column(name = "selection_source", nullable = false, length = 16)
  private String selectionSource;

  @Column(name = "reason_code", nullable = false, length = 64)
  private String reasonCode;

  @Column(name = "selected_by", nullable = false, length = 255)
  private String selectedBy;

  @Column(name = "selected_at", nullable = false)
  private LocalDateTime selectedAt;

  public static MfdsMatchingSelection adminSelect(
      Long declarationId,
      String targetType,
      Long targetId,
      String reasonCode,
      Long adminId,
      LocalDateTime selectedAt) {
    return adminSelection(
        declarationId, targetType, targetId, "SELECT", reasonCode, adminId, selectedAt);
  }

  public static MfdsMatchingSelection adminRevoke(
      Long declarationId,
      String targetType,
      Long targetId,
      String reasonCode,
      Long adminId,
      LocalDateTime selectedAt) {
    return adminSelection(
        declarationId, targetType, targetId, "REVOKE", reasonCode, adminId, selectedAt);
  }

  private static MfdsMatchingSelection adminSelection(
      Long declarationId,
      String targetType,
      Long targetId,
      String action,
      String reasonCode,
      Long adminId,
      LocalDateTime selectedAt) {
    MfdsMatchingSelection selection = new MfdsMatchingSelection();
    // 관리자 선택은 자동 실행에 속하지 않으므로 run_id를 비워 반복 확정 이력을 보존한다.
    selection.declarationId = declarationId;
    selection.targetType = targetType;
    selection.targetId = targetId;
    selection.action = action;
    selection.selectionSource = "ADMIN";
    selection.reasonCode = reasonCode;
    selection.selectedBy = Objects.requireNonNull(adminId, "adminId").toString();
    selection.selectedAt = selectedAt;
    return selection;
  }
}
