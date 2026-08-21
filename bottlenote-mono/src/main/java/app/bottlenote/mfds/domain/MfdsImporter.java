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

  @Comment("수입사 ID")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("식약처 공식 업소 식별 코드")
  @Column(name = "official_business_code", nullable = false, length = 32, unique = true)
  private String officialBusinessCode;

  @Comment("수입사 인허가 번호")
  @Column(name = "license_no", nullable = false, length = 64)
  private String licenseNo;

  @Comment("공식 수입사명")
  @Column(name = "business_name", nullable = false, length = 512)
  private String businessName;

  @Comment("수입사명 정규화 SHA-256")
  @Column(name = "business_name_key_sha256", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] businessNameKeySha256;

  @Comment("대표자명")
  @Column(name = "representative_name", length = 255)
  private String representativeName;

  @Comment("인허가 일자")
  @Column(name = "permit_date")
  private LocalDate permitDate;

  @Comment("인허가 기관명")
  @Column(name = "institution_name", length = 255)
  private String institutionName;

  @Comment("수입사 주소")
  @Column(name = "primary_address", columnDefinition = "TEXT")
  private String primaryAddress;

  @Comment("수입사 전화번호")
  @Column(name = "telephone_no", length = 64)
  private String telephoneNo;

  @Comment("인허가 업종명")
  @Column(name = "industry_name", length = 255)
  private String industryName;

  @Comment("공식 영업 상태")
  @Column(name = "operating_status", nullable = false, length = 32)
  private String operatingStatus;

  @Comment("수입사 목록 출처 URL")
  @Column(name = "source_list_url", nullable = false, columnDefinition = "TEXT")
  private String sourceListUrl;

  @Comment("수입사 상세 출처 URL")
  @Column(name = "source_detail_url", columnDefinition = "TEXT")
  private String sourceDetailUrl;

  @Comment("수입사 목록 원문 SHA-256")
  @Column(name = "source_list_sha256", nullable = false, columnDefinition = "BINARY(32)")
  private byte[] sourceListSha256;

  @Comment("수입사 상세 원문 SHA-256")
  @Column(name = "source_detail_sha256", columnDefinition = "BINARY(32)")
  private byte[] sourceDetailSha256;

  @Comment("공식 출처 확인 시각")
  @Column(name = "source_observed_at", nullable = false)
  private LocalDateTime sourceObservedAt;

  @Comment("수입사 공개 설명")
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Comment("관리자 내부 메모")
  @Column(name = "admin_note", columnDefinition = "TEXT")
  private String adminNote;

  @Comment("관리 상태")
  @Enumerated(EnumType.STRING)
  @Column(name = "admin_status", nullable = false, length = 32)
  private MfdsImporterAdminStatus adminStatus;

  @Comment("최종 검토자")
  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Comment("최종 검토 시각")
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Comment("생성 시각")
  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Comment("수정 시각")
  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  /**
   * 관리자가 확정한 수입사를 등록한다.
   *
   * <p>businessNameKeySha256은 TRIM(업소명)의 SHA-256, sourceListSha256은 원문 HTML이 없어 출처 URL 문자열의
   * SHA-256으로 대체한다.
   */
  public static MfdsImporter create(
      String officialBusinessCode,
      String licenseNo,
      String businessName,
      String representativeName,
      String sourceListUrl,
      String description,
      String adminNote,
      MfdsImporterAdminStatus adminStatus) {
    MfdsImporter importer = new MfdsImporter();
    importer.officialBusinessCode = officialBusinessCode;
    importer.licenseNo = licenseNo;
    importer.businessName = businessName.trim();
    importer.businessNameKeySha256 = sha256(importer.businessName);
    importer.representativeName = representativeName;
    importer.operatingStatus = "UNKNOWN";
    importer.sourceListUrl = sourceListUrl;
    importer.sourceListSha256 = sha256(sourceListUrl);
    importer.sourceObservedAt = LocalDateTime.now();
    importer.description = description;
    importer.adminNote = adminNote;
    importer.adminStatus = adminStatus != null ? adminStatus : MfdsImporterAdminStatus.ACTIVE;
    return importer;
  }

  /** 관리자 관리 항목(공식명·설명·메모·관리 상태)을 수정한다. 업소명이 바뀌면 자동 매칭 키를 다시 계산한다. */
  public void update(
      String businessName,
      String description,
      String adminNote,
      MfdsImporterAdminStatus adminStatus) {
    String trimmedName = businessName.trim();
    if (!trimmedName.equals(this.businessName)) {
      this.businessName = trimmedName;
      this.businessNameKeySha256 = sha256(trimmedName);
    }
    this.description = description;
    this.adminNote = adminNote;
    this.adminStatus = adminStatus;
  }

  private static byte[] sha256(String value) {
    try {
      return java.security.MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
    }
  }
}
