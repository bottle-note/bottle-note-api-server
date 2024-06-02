package app.bottlenote.review.domain;

import app.bottlenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Comment("리뷰 테이스팅 태그 테이블")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "review_tasting_tag")
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

	@Builder
	public ReviewTastingTag(Long id, Review review, String tastingTag) {
		this.id = id;
		this.review = review;
		this.tastingTag = tastingTag;
	}
}

