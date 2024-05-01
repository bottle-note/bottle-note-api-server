package app.bottlenote.rating.domain;

import app.bottlenote.rating.exception.RatingException;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import static app.bottlenote.rating.exception.RatingExceptionCode.INVALID_RATING_POINT;

@Getter
@Embeddable
public class RatingPoint {

	private final Double rating;

	public RatingPoint() {
		this(0.0);
	}

	private RatingPoint(double rating) {
		if (!isValidRating(rating))
			throw new RatingException(INVALID_RATING_POINT);
		this.rating = rating;
	}

	public static RatingPoint of(double rating) {
		return new RatingPoint(rating);
	}

	/**
	 * 이 VO는 0.0/1.0/1.5/2.0/2.5/3.0/3.5/4.0/4.5/5.0 중 하나의 값을 가질 수 있습니다.
	 *
	 * @param rating the rating
	 * @return the boolean
	 */
	private boolean isValidRating(double rating) {
		return rating >= 0.0 && rating <= 5.0 && (rating * 2) % 1 == 0;
	}

	/**
	 * 이 VO는 x.x 형태로 출력됩니다.l
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return String.format("%.1f", rating);
	}
}
