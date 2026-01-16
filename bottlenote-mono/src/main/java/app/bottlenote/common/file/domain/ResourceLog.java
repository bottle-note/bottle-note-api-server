package app.bottlenote.common.file.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.common.file.constant.ResourceEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "resource_log")
@Table(name = "resource_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("요청 사용자 ID")
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Comment("리소스 키 (S3 객체 키 등)")
  @Column(name = "resource_key", nullable = false, length = 1024, unique = true)
  private String resourceKey;

  @Comment("리소스 타입: IMAGE")
  @Column(name = "resource_type", nullable = false, length = 64)
  private String resourceType;

  @Comment("이벤트 타입: CREATED/ACTIVATED/INVALIDATED/DELETED")
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 32)
  private ResourceEventType eventType;

  @Comment("연결된 엔티티 ID")
  @Column(name = "reference_id")
  private Long referenceId;

  @Comment("연결 타입: REVIEW/PROFILE/HELP")
  @Column(name = "reference_type", length = 64)
  private String referenceType;

  @Comment("조회 URL")
  @Column(name = "view_url", length = 2048)
  private String viewUrl;

  @Comment("저장 경로")
  @Column(name = "root_path", length = 255)
  private String rootPath;

  @Comment("버킷명")
  @Column(name = "bucket_name", length = 128)
  private String bucketName;

  @Builder
  public ResourceLog(
      Long id,
      Long userId,
      String resourceKey,
      String resourceType,
      ResourceEventType eventType,
      Long referenceId,
      String referenceType,
      String viewUrl,
      String rootPath,
      String bucketName) {
    this.id = id;
    this.userId = userId;
    this.resourceKey = resourceKey;
    this.resourceType = resourceType;
    this.eventType = eventType;
    this.referenceId = referenceId;
    this.referenceType = referenceType;
    this.viewUrl = viewUrl;
    this.rootPath = rootPath;
    this.bucketName = bucketName;
  }

  /** 리소스를 활성화 상태로 변경합니다. */
  public ResourceLog activate(Long referenceId, String referenceType) {
    validateStateTransition(ResourceEventType.ACTIVATED);
    this.eventType = ResourceEventType.ACTIVATED;
    this.referenceId = referenceId;
    this.referenceType = referenceType;
    return this;
  }

  /** 리소스를 무효화 상태로 변경합니다. */
  public ResourceLog invalidate() {
    validateStateTransition(ResourceEventType.INVALIDATED);
    this.eventType = ResourceEventType.INVALIDATED;
    return this;
  }

  /** 리소스를 삭제 상태로 변경합니다. */
  public ResourceLog markDeleted() {
    validateStateTransition(ResourceEventType.DELETED);
    this.eventType = ResourceEventType.DELETED;
    return this;
  }

  private void validateStateTransition(ResourceEventType newState) {
    if (!canTransitionTo(newState)) {
      throw new IllegalStateException(
          String.format("%s 상태에서 %s 상태로 전이할 수 없습니다.", this.eventType, newState));
    }
  }

  /** 현재 상태에서 새 상태로 전이가 가능한지 확인합니다. */
  public boolean canTransitionTo(ResourceEventType newState) {
    return switch (this.eventType) {
      case CREATED ->
          newState == ResourceEventType.ACTIVATED || newState == ResourceEventType.INVALIDATED;
      case ACTIVATED -> newState == ResourceEventType.INVALIDATED;
      case INVALIDATED -> newState == ResourceEventType.DELETED;
      case DELETED -> false;
    };
  }

  public boolean isActivated() {
    return this.eventType == ResourceEventType.ACTIVATED;
  }

  public boolean isInvalidated() {
    return this.eventType == ResourceEventType.INVALIDATED;
  }
}
