package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@Entity(name = "alcohol")
@Table(name = "alcohols")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alcohol extends BaseEntity {

  @Id
  @Comment("알코올 ID")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("알코올 한글 이름")
  @Column(name = "kor_name", nullable = false)
  private String korName;

  @Comment("알코올 영어 이름")
  @Column(name = "eng_name", nullable = false)
  private String engName;

  @Comment("도수")
  @Column(name = "abv")
  private String abv;

  @Comment("타입")
  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AlcoholType type;

  @Comment("하위 카테고리 한글명 ( ex. 위스키, 럼 )")
  @Column(name = "kor_category", nullable = false)
  private String korCategory;

  @Comment("하위 카테고리 영문명 ( ex. 위스키, 럼 )")
  @Column(name = "eng_category", nullable = false)
  private String engCategory;

  @Comment("하위 카테고리 그룹")
  @Enumerated(EnumType.STRING)
  @Column(name = "category_group", nullable = false)
  private AlcoholCategoryGroup categoryGroup;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region_id")
  private Region region;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "distillery_id")
  private Distillery distillery;

  @Comment("숙성년도")
  @Column(name = "age")
  private String age;

  @Comment("캐스트 타입")
  @Column(name = "cask")
  private String cask;

  @Comment("썸네일 이미지")
  @Column(name = "image_url")
  private String imageUrl;

  @Comment("기본 설명")
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Comment("용량")
  @Column(name = "volume")
  private String volume;

  @Builder.Default
  @OneToMany(mappedBy = "alcohol", fetch = FetchType.LAZY)
  private Set<AlcoholsTastingTags> alcoholsTastingTags = new HashSet<>();
}
