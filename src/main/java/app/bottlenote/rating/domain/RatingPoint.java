package app.bottlenote.rating.domain;

import app.bottlenote.rating.exception.RatingException;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.io.Serializable;

import static app.bottlenote.rating.exception.RatingExceptionCode.INVALID_RATING_POINT;

/**
 * 평점을 나타내는 VO입니다.
 */
@Getter
@Embeddable
public class RatingPoint implements Serializable {

	private final Double rating;

	public RatingPoint() {
		this(0.0);
	}

	private RatingPoint(double rating) {
		if (isNotValidRating(rating))
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

	@JsonCreator
	public static RatingPoint of(String rating) {
		return new RatingPoint(Integer.parseInt(rating));
	}

	/**
	 * 평점이 유효한지 확인합니다.
	 *
	 * @param rating the rating
	 */
	public void isValidRating(double rating) {
		if (isNotValidRating(rating))
			throw new RatingException(INVALID_RATING_POINT);
	}

	/**
	 * 0.0 ~ 5.0 사이의 값인지 확인합니다.
	 * True : 유효한 값
	 * False : 유효하지 않은 값
	 */
	private boolean isNotValidRating(double rating) {
		return !isWithinValidRange(rating) || !isIncrementOfHalf(rating);
	}

	/**
	 * 0.0 ~ 5.0 사이의 값인지 확인합니다.
	 */
	private boolean isWithinValidRange(double rating) {
		return rating >= 0.0 && rating <= 5.0;
	}

	/**
	 * 0.5 단위로 증가하는지 확인합니다.
	 */
	private boolean isIncrementOfHalf(double rating) {
		double multiplied = rating * 2;
		return multiplied == Math.floor(multiplied);
	}

	@Override
	public String toString() {
		return String.format("%.1f", rating);
	}
}
