package app.bottlenote.banner.domain;

import app.bottlenote.banner.constant.BannerType;
import app.bottlenote.banner.constant.TextPosition;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("홈 배너")
@Entity(name = "banner")
@Table(name = "banners")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Banner extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("배너명")
  @Column(name = "name", nullable = false)
  private String name;

  @Comment("배너명 텍스트 색상(hex)")
  @Column(name = "name_font_color", nullable = false, length = 7)
  @Builder.Default
  private String nameFontColor = "#ffffff";

  @Comment("배너 설명 부분1")
  @Column(name = "description_a", length = 50)
  private String descriptionA;

  @Comment("배너 설명 부분2")
  @Column(name = "description_b", length = 50)
  private String descriptionB;

  @Comment("배너 설명 텍스트 색상(HEX) a,b 동일 적용")
  @Column(name = "description_font_color", nullable = false, length = 7)
  @Builder.Default
  private String descriptionFontColor = "#ffffff";

  @Comment("이미지 URL")
  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Comment("텍스트 위치")
  @Column(name = "text_position", nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private TextPosition textPosition = TextPosition.RT;

  @Comment("외부 URL 여부")
  @Column(name = "is_external_url", nullable = false)
  @Builder.Default
  private Boolean isExternalUrl = false;

  @Comment("클릭 시 이동 URL")
  @Column(name = "target_url")
  private String targetUrl;

  @Comment("배너 유형")
  @Column(name = "banner_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private BannerType bannerType;

  @Comment("정렬 순서")
  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Comment("노출 시작일")
  @Column(name = "start_date")
  private LocalDateTime startDate;

  @Comment("노출 종료일")
  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Comment("활성화 여부")
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  public void update(
      String name,
      String nameFontColor,
      String descriptionA,
      String descriptionB,
      String descriptionFontColor,
      String imageUrl,
      TextPosition textPosition,
      Boolean isExternalUrl,
      String targetUrl,
      BannerType bannerType,
      Integer sortOrder,
      LocalDateTime startDate,
      LocalDateTime endDate,
      Boolean isActive) {
    this.name = name;
    this.nameFontColor = nameFontColor;
    this.descriptionA = descriptionA;
    this.descriptionB = descriptionB;
    this.descriptionFontColor = descriptionFontColor;
    this.imageUrl = imageUrl;
    this.textPosition = textPosition;
    this.isExternalUrl = isExternalUrl;
    this.targetUrl = targetUrl;
    this.bannerType = bannerType;
    this.sortOrder = sortOrder;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isActive = isActive;
  }

  public void updateStatus(Boolean isActive) {
    this.isActive = isActive;
  }

  public void updateSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}
