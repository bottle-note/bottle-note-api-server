package app.bottlenote.support.business.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.common.image.ImageInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
@Entity(name = "business_image")
@Table(name = "business_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessImage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private ImageInfo businessImageInfo;

  @Comment("비즈니스 문의 아이디")
  @Column(name = "business_support_id", nullable = false)
  private Long businessSupportId;

  @Builder
  public BusinessImage(Long id, ImageInfo businessImageInfo, Long businessSupportId) {
    this.id = id;
    this.businessImageInfo = businessImageInfo;
    this.businessSupportId = businessSupportId;
  }
}
