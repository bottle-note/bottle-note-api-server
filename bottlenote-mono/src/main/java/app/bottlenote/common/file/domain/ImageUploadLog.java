package app.bottlenote.common.file.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.common.file.constant.ImageUploadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "image_upload_log")
@Table(name = "image_upload_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageUploadLog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("업로드 요청 사용자 ID")
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Comment("S3 객체 키")
  @Column(name = "image_key", nullable = false, length = 1024)
  private String imageKey;

  @Comment("CloudFront 조회 URL")
  @Column(name = "view_url", nullable = false, length = 2048)
  private String viewUrl;

  @Comment("상태: PENDING/UPLOADED/CONFIRMED/DELETED")
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ImageUploadStatus status;

  @Comment("연결된 엔티티 ID")
  @Column(name = "reference_id")
  private Long referenceId;

  @Comment("저장 경로")
  @Column(name = "root_path", length = 255)
  private String rootPath;

  @Comment("MIME 타입")
  @Column(name = "content_type", length = 128)
  private String contentType;

  @Comment("파일 크기 bytes")
  @Column(name = "content_length")
  private Long contentLength;

  @Comment("원본 파일명")
  @Column(name = "original_file_name", length = 512)
  private String originalFileName;

  @Comment("S3 버킷명")
  @Column(name = "bucket_name", length = 128)
  private String bucketName;

  @Comment("MD5 해시")
  @Column(name = "etag", length = 128)
  private String etag;

  @Comment("사용 확정 시간")
  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Comment("연결 타입: REVIEW/PROFILE/HELP")
  @Column(name = "reference_type", length = 64)
  private String referenceType;

  @Builder
  public ImageUploadLog(
      Long id,
      Long userId,
      String imageKey,
      String viewUrl,
      ImageUploadStatus status,
      Long referenceId,
      String rootPath,
      String contentType,
      Long contentLength,
      String originalFileName,
      String bucketName,
      String etag,
      LocalDateTime confirmedAt,
      String referenceType) {
    this.id = id;
    this.userId = userId;
    this.imageKey = imageKey;
    this.viewUrl = viewUrl;
    this.status = status;
    this.referenceId = referenceId;
    this.rootPath = rootPath;
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.originalFileName = originalFileName;
    this.bucketName = bucketName;
    this.etag = etag;
    this.confirmedAt = confirmedAt;
    this.referenceType = referenceType;
  }

  public void confirm(Long referenceId, String referenceType) {
    this.referenceId = referenceId;
    this.referenceType = referenceType;
    this.status = ImageUploadStatus.CONFIRMED;
    this.confirmedAt = LocalDateTime.now();
  }

  public void markAsDeleted() {
    this.status = ImageUploadStatus.DELETED;
  }

  public void updateUploadInfo(String etag, Long contentLength, String contentType) {
    this.etag = etag;
    this.contentLength = contentLength;
    this.contentType = contentType;
    this.status = ImageUploadStatus.UPLOADED;
  }
}
