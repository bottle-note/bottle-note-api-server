package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;

@Getter
@Builder
@ToString(exclude = "alcoholsTastingTags")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tasting_tag")
@Table(name = "tasting_tags")
public class TastingTag extends BaseEntity {
  @Id
  @Comment("태그 ID")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("태그 영어 이름")
  @Column(name = "eng_name", nullable = false)
  private String engName;

  @Comment("태그 한글 이름")
  @Column(name = "kor_name", nullable = false)
  private String korName;

  @Lob
  @Comment("아이콘 (Base64 이미지)")
  @Column(name = "icon", columnDefinition = "MEDIUMTEXT")
  private String icon;

  @Comment("태그 설명")
  @Column(name = "description")
  private String description;

  @Comment("부모 태그 ID (null이면 root)")
  @Column(name = "parent_id")
  private Long parentId;

  @Builder.Default
  @OneToMany(mappedBy = "tastingTag")
  private List<AlcoholsTastingTags> alcoholsTastingTags = new ArrayList<>();

  public void update(
      String korName, String engName, String icon, String description, Long parentId) {
    this.korName = korName;
    this.engName = engName;
    this.icon = icon;
    this.description = description;
    this.parentId = parentId;
  }

  public boolean isRoot() {
    return this.parentId == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TastingTag that = (TastingTag) o;
    return Objects.equals(korName, that.korName) && Objects.equals(engName, that.engName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(korName, engName);
  }
}
