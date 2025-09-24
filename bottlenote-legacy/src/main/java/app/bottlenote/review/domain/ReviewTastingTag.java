package app.bottlenote.review.domain;

import app.bottlenote.core.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@Comment("리뷰 테이스팅 태그 테이블")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity(name = "review_tasting_tag")
@Table(name = "review_tasting_tags")
public class ReviewTastingTag extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id")
  private Review review;

  @Comment("테이스팅 태그")
  @Column(name = "tasting_tag")
  private String tastingTag;

  public static ReviewTastingTag create(Review review, String tastingTag) {
    return ReviewTastingTag.builder().review(review).tastingTag(tastingTag).build();
  }
}
