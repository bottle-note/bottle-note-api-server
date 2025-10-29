package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("증류소")
@Entity(name = "distillery")
@Table(name = "distilleries")
public class Distillery extends BaseEntity {

  @Id
  @Comment("증류소 ID")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("증류소 영어 이름")
  @Column(name = "eng_name", nullable = false)
  private String engName;

  @Comment("증류소 한글 이름")
  @Column(name = "kor_name", nullable = false)
  private String korName;

  @Comment("증류소 로고 이미지 경로")
  @Column(name = "logo_img_url")
  private String logoImgPath;

  @Builder.Default
  @OneToMany(mappedBy = "distillery")
  private List<Alcohol> alcohol = new ArrayList<>();
}
