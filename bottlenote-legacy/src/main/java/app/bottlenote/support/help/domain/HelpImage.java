package app.bottlenote.support.help.domain;

import app.bottlenote.common.image.ImageInfo;
import app.bottlenote.core.common.domain.BaseEntity;
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
@Entity(name = "help_image")
@Table(name = "help_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HelpImage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  @Comment("문의글 이미지")
  private ImageInfo helpimageInfo;

  @Comment("문의글 아이디")
  @Column(name = "help_id", nullable = false)
  private Long helpId;

  @Builder
  public HelpImage(Long id, ImageInfo helpimageInfo, Long helpId) {
    this.id = id;
    this.helpimageInfo = helpimageInfo;
    this.helpId = helpId;
  }
}
