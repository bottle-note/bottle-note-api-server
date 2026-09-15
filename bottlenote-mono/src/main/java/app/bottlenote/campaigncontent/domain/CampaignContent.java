package app.bottlenote.campaigncontent.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("캠페인 콘텐츠")
@Entity(name = "campaign_content")
@Table(name = "campaign_contents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CampaignContent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("FE가 이벤트에 싣는 코드, 등록 후 변경 불가")
  @Column(name = "code", nullable = false, length = 50, updatable = false)
  private String code;

  @Comment("캠페인 콘텐츠 이름")
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Comment("캠페인 콘텐츠 설명")
  @Column(name = "description")
  private String description;

  @Comment("활성 여부")
  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  public void update(String name, String description, Boolean isActive) {
    this.name = name;
    this.description = description;
    this.isActive = isActive;
  }

  public void updateStatus(Boolean isActive) {
    this.isActive = isActive;
  }
}
