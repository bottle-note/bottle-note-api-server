package app.bottlenote.rating.domain;

import app.bottlenote.rating.exception.RatingException;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import static app.bottlenote.rating.exception.RatingExceptionCode.INVALID_RATING_POINT;

/**
 * 평점을 나타내는 VO입니다.
 */
@Embeddable
@Getter
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

	@JsonCreator
	public static RatingPoint of(Double rating) {
		return new RatingPoint(rating);
	}

	@JsonCreator
	public static RatingPoint of(Integer rating) {
		return new RatingPoint(rating);
	}

	/**
	 * 0.0 ~ 5.0 사이의 값인지 확인합니다.
	 */
	private static boolean isValidRating(double rating) {
		return isWithinValidRange(rating) && isIncrementOfHalf(rating);
	}

	/**
	 * 0.0 ~ 5.0 사이의 값인지 확인합니다.
	 */
	private static boolean isWithinValidRange(double rating) {
		return rating >= 0.0 && rating <= 5.0;
	}

	/**
	 * 0.5 단위로 증가하는지 확인합니다.
	 */
	private static boolean isIncrementOfHalf(double rating) {
		double multiplied = rating * 2;
		return multiplied == Math.floor(multiplied);
	}

	@Override
	public String toString() {
		return String.format("%.1f", rating);
	}
}
