package app.bottlenote.global.service.converter;

import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.exception.RatingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static app.bottlenote.rating.exception.RatingExceptionCode.INPUT_NUMBER_IS_NOT_A_NUMBER;
import static app.bottlenote.rating.exception.RatingExceptionCode.INPUT_VALUE_IS_NOT_VALID;

@Slf4j
@Component
public class RatingPointConverter implements Converter<String, RatingPoint> {
	@Override
	public RatingPoint convert(@NonNull String source) {
		try {
			double rating = Double.parseDouble(source);
			return RatingPoint.of(rating);
		} catch (NumberFormatException e) {
			log.warn("입력된 값이 숫자가 아닙니다: {}", source);
			throw new RatingException(INPUT_NUMBER_IS_NOT_A_NUMBER);
		} catch (IllegalArgumentException e) {
			log.warn("유효하지 않은 별점 값: {}. 별점은 0.0부터 5.0 사이이며, 0.5 단위로 증가해야 합니다.", source);
			throw new RatingException(INPUT_VALUE_IS_NOT_VALID);
		}
	}
}
