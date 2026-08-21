package app.bottlenote.mfds.domain;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "mfds_importer_rcno_link")
@Table(name = "mfds_importer_rcno_links")
@Comment("공식 화면에서 확인한 RCNO별 수입사 연결 근거")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfdsImporterRcnoLink {

  @Id
  @Column(name = "rcno", nullable = false, length = 32)
  private String rcno;

  @Column(name = "importer_id", nullable = false)
  private Long importerId;

  @Column(name = "source_importer_name", nullable = false, length = 512)
  private String sourceImporterName;

  @Enumerated(EnumType.STRING)
  @Column(name = "link_source", nullable = false, length = 16)
  private MfdsImporterLinkSource linkSource;

  @Column(name = "source_gallery_url", columnDefinition = "TEXT")
  private String sourceGalleryUrl;

  @Column(name = "source_gallery_sha256", columnDefinition = "BINARY(32)")
  private byte[] sourceGallerySha256;

  @Column(name = "source_observed_at")
  private LocalDateTime sourceObservedAt;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
