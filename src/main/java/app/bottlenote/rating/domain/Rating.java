package app.bottlenote.rating.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;

@Getter
@ToString(exclude = {"alcohol", "user"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("알콜 점수 테이블")
@Entity(name = "rating")
public class Rating extends BaseEntity {

	@EmbeddedId
	private RatingId id;

	@MapsId("alcoholId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohol_id")
	private Alcohol alcohol;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Embedded
	@Comment("평가점수 : 0, 0.5, 1.0 ... 5.0")
	@Column(name = "rating")
	private RatingPoint ratingPoint = new RatingPoint();


	@Builder
	public Rating(RatingId id, RatingPoint ratingPoint, Alcohol alcohol, User user) {
		this.id = id;
		this.ratingPoint = ratingPoint;
		this.alcohol = alcohol;
		this.user = user;
	}

	public void registerRatingPoint(RatingPoint ratingPoint) {
		ratingPoint.isValidRating(ratingPoint.getRating());
		this.ratingPoint = ratingPoint;
	}
}
