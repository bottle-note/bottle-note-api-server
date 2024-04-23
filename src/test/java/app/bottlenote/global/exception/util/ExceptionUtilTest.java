package app.bottlenote.global.exception.util;

import org.junit.jupiter.api.Test;

import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessage;
import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessageForTypeMismatch;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionUtilTest {

	@Test
	void 에러_메시지를_반환받을수_있다() {
		// Given
		String defaultMessage = "fieldName";
		Object rejectedValue = "rejectedValue";

		// When
		String result = getErrorMessage(rejectedValue, defaultMessage);

		// Then
		// return defaultMessage != null ? defaultMessage : "유효하지 않은 파라미터입니다: " + " (" + rejectedValue + ")";
		String expected = "fieldName";
		assertEquals(expected, result);
	}

	@Test
	void defaultMessage가_null일_경우_기본값이_반환된다() {
		// Given
		String defaultMessage = null;
		Object rejectedValue = "rejectedValue";

		// When
		String result = getErrorMessage(rejectedValue, defaultMessage);

		// Then
		// return defaultMessage != null ? defaultMessage : "유효하지 않은 파라미터입니다: " + " (" + rejectedValue + ")";
		String expected = "유효하지 않은 파라미터입니다:  (rejectedValue)";
		assertEquals(expected, result);
	}

	@Test
	void 에러_타입과_메시지를_반환받을수_있다() {
		// Given
		String errorType = "errorType";
		String fieldName = "fieldName";
		Object rejectedValue = "rejectedValue";

		// When
		String result = getErrorMessage(errorType, fieldName, rejectedValue);

		// Then
		String expected = "errorType: fieldName [rejectedValue]";
		assertEquals(expected, result);
	}

	@Test
	void 타입_불일치_에러_메시지를_반환받을수_있다() {
		// Given
		String fieldName = "fieldName";
		String rejectedValue = "rejectedValue";
		String requiredType = "requiredType";

		// When
		String result = getErrorMessageForTypeMismatch(fieldName, rejectedValue, requiredType);

		// Then
		String expected = "'fieldName' 필드는 'requiredType' 타입이 필요하지만, 잘못된 값 'rejectedValue'이(가) 제공되었습니다.";
		assertEquals(expected, result);
	}
}
