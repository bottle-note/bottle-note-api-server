package app.bottlenote.mfds.domain;

import app.bottlenote.mfds.constant.MfdsImporterAdminStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "mfds_importer")
@Table(name = "mfds_importers")
@Comment("식약처 공식 수입사")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfdsImporter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "official_business_code", nullable = false, length = 32, unique = true)
  private String officialBusinessCode;

  @Column(name = "license_no", nullable = false, length = 64)
  private String licenseNo;

  @Column(name = "business_name", nullable = false, length = 512)
  private String businessName;

  @Column(name = "business_name_key_sha256", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] businessNameKeySha256;

  @Column(name = "representative_name", length = 255)
  private String representativeName;

  @Column(name = "permit_date")
  private LocalDate permitDate;

  @Column(name = "institution_name", length = 255)
  private String institutionName;

  @Column(name = "primary_address", columnDefinition = "TEXT")
  private String primaryAddress;

  @Column(name = "telephone_no", length = 64)
  private String telephoneNo;

  @Column(name = "industry_name", length = 255)
  private String industryName;

  @Column(name = "operating_status", nullable = false, length = 32)
  private String operatingStatus;

  @Column(name = "source_list_url", nullable = false, columnDefinition = "TEXT")
  private String sourceListUrl;

  @Column(name = "source_detail_url", columnDefinition = "TEXT")
  private String sourceDetailUrl;

  @Column(name = "source_list_sha256", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] sourceListSha256;

  @Column(name = "source_detail_sha256", columnDefinition = "BINARY(32)")
  private byte[] sourceDetailSha256;

  @Column(name = "source_observed_at", nullable = false)
  private LocalDateTime sourceObservedAt;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "admin_note", columnDefinition = "TEXT")
  private String adminNote;

  @Enumerated(EnumType.STRING)
  @Column(name = "admin_status", nullable = false, length = 32)
  private MfdsImporterAdminStatus adminStatus;

  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}
