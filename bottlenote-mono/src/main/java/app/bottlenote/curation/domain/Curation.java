package app.bottlenote.curation.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("spec 기반 큐레이션")
@Entity(name = "curation")
@Table(name = "curation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Curation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("큐레이션 스펙 ID")
  @Column(name = "spec_id", nullable = false)
  private Long specId;

  @Comment("큐레이션명")
  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Comment("큐레이션 설명")
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Comment("대표 이미지 URL")
  @Column(name = "cover_image_url", nullable = false, length = 2048)
  private String coverImageUrl;

  @Comment("추가 이미지 URL 2")
  @Column(name = "image_url_2", length = 2048)
  private String imageUrl2;

  @Comment("추가 이미지 URL 3")
  @Column(name = "image_url_3", length = 2048)
  private String imageUrl3;

  @Comment("노출 시작일")
  @Column(name = "exposure_start_date")
  private LocalDate exposureStartDate;

  @Comment("노출 종료일")
  @Column(name = "exposure_end_date")
  private LocalDate exposureEndDate;

  @Builder.Default
  @Comment("노출 순서")
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  @Builder.Default
  @Comment("활성화 여부")
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  public void update(
      Long specId,
      String name,
      String description,
      String coverImageUrl,
      String imageUrl2,
      String imageUrl3,
      LocalDate exposureStartDate,
      LocalDate exposureEndDate,
      Integer displayOrder,
      Boolean isActive) {
    this.specId = specId;
    this.name = name;
    this.description = description;
    this.coverImageUrl = coverImageUrl;
    this.imageUrl2 = imageUrl2;
    this.imageUrl3 = imageUrl3;
    this.exposureStartDate = exposureStartDate;
    this.exposureEndDate = exposureEndDate;
    this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
    this.isActive = isActive != null ? isActive : this.isActive;
  }

  public void updateStatus(Boolean isActive) {
    this.isActive = isActive;
  }

  public void updateDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }
}
