package app.bottlenote.rating.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("알콜 점수 테이블")
@Entity(name = "rating")
public class Rating extends BaseEntity {

	@EmbeddedId
	private RatingId id;

	@Embedded
	@Comment("평가점수 : 0, 0.5, 1.0 ... 5.0")
	@Column(name = "rating")
	private RatingPoint ratingPoint = new RatingPoint();

	@Builder
	public Rating(RatingId id, RatingPoint ratingPoint) {
		this.id = id;
		this.ratingPoint = ratingPoint;
	}

	public void registerRatingPoint(RatingPoint ratingPoint) {
		ratingPoint.isValidRating(ratingPoint.getRating());
		this.ratingPoint = ratingPoint;
	}
}
