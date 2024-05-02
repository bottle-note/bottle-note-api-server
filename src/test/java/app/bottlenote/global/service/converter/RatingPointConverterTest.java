package app.bottlenote.global.service.converter;

import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.exception.RatingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DisplayName(value = "RatingPoint Converter 테스트")
class RatingPointConverterTest {

	RatingPointConverter ratingPointConverter = new RatingPointConverter();

	@DisplayName(value = "정상 평점을 입력할 경우 RatingPoint 객체로 변환한다.")
	@ParameterizedTest
	@ValueSource(strings = {"1.0", "2.0", "3.0", "4.0", "5.0", "1.5", "2.5", "3.5", "4.5", "0.5"})
	void convertRatingPoint(final String ratingPoint) {
		// when
		RatingPoint convert = ratingPointConverter.convert(ratingPoint);

		// then
		assertThat(Objects.requireNonNull(convert).getRating()).isEqualTo(Double.parseDouble(ratingPoint));
	}

	@DisplayName(value = "적절하지 않은 평점을 입력할 경우 예외가 발생하다.")
	@ParameterizedTest
	@ValueSource(strings = {"-1.0", "-2.0", "-3.0", "24.0", "15.0", "-1.5", "-2.5", "-3.5", "-4.5", "-0.5"})
	void convertRatingPointException(final String ratingPoint) {
		// when
		// then
		assertThatExceptionOfType(RatingException.class)
			.isThrownBy(() -> ratingPointConverter.convert(ratingPoint));
	}


}
