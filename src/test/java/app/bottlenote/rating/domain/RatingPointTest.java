package app.bottlenote.rating.domain;

import app.bottlenote.rating.exception.RatingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("별점  테스트")
class RatingPointTest {


	@Test
	@DisplayName("평가 점수가 0.0, 3.0, 5.0일 때 정상적으로 생성되어야 한다.")
	void testValidRating() {
		assertDoesNotThrow(() -> RatingPoint.of(3.0));
		assertDoesNotThrow(() -> RatingPoint.of(0.0));
		assertDoesNotThrow(() -> RatingPoint.of(5.0));
	}

	@Test
	@DisplayName("평가 점수가 0.0 미만, 5.0 초과, 0.5 단위가 아닌 경우 예외가 발생해야 한다.")
	void testInvalidRating() {
		assertThrows(RatingException.class, () -> RatingPoint.of(-1.0));
		assertThrows(RatingException.class, () -> RatingPoint.of(5.5));
		assertThrows(RatingException.class, () -> RatingPoint.of(3.3));
	}
}
