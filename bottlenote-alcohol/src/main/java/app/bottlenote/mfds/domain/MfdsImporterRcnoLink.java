package app.bottlenote.mfds.domain;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @Comment("수입신고번호")
  @Id
  @Column(name = "rcno", nullable = false, length = 32)
  private String rcno;

  @Comment("연결된 수입사 ID")
  @Column(name = "importer_id", nullable = false)
  private Long importerId;

  @Comment("연결된 수입사")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "importer_id", insertable = false, updatable = false)
  private MfdsImporter importer;

  @Comment("원장에 표시된 수입사명")
  @Column(name = "source_importer_name", nullable = false, length = 512)
  private String sourceImporterName;

  @Comment("수입사 연결 근거 유형")
  @Enumerated(EnumType.STRING)
  @Column(name = "link_source", nullable = false, length = 16)
  private MfdsImporterLinkSource linkSource;

  @Comment("공식 제품 상세 출처 URL")
  @Column(name = "source_gallery_url", columnDefinition = "TEXT")
  private String sourceGalleryUrl;

  @Comment("공식 제품 상세 원문 SHA-256")
  @Column(name = "source_gallery_sha256", columnDefinition = "BINARY(32)")
  private byte[] sourceGallerySha256;

  @Comment("공식 출처 확인 시각")
  @Column(name = "source_observed_at")
  private LocalDateTime sourceObservedAt;

  @Comment("생성 시각")
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Comment("수정 시각")
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /** 관리자 확인 등으로 확보한 RCNO별 수입사 연결 근거를 생성한다. */
  public static MfdsImporterRcnoLink create(
      String rcno, Long importerId, String sourceImporterName, MfdsImporterLinkSource linkSource) {
    MfdsImporterRcnoLink link = new MfdsImporterRcnoLink();
    link.rcno = rcno;
    link.importerId = importerId;
    link.sourceImporterName = sourceImporterName;
    link.linkSource = linkSource;
    link.sourceObservedAt = LocalDateTime.now();
    return link;
  }
}
