package app.bottlenote.global.exception.util;

import org.junit.jupiter.api.Test;

import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessage;
import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessageForTypeMismatch;

class ExceptionUtilTest {

	@Test
	void 에러_메시지를_반환받을수_있다() {
		// Given
		String fieldName = "fieldName";
		Object rejectedValue = "rejectedValue";

		// When
		String result = getErrorMessage(fieldName, rejectedValue);

		// Then
		String expected = "유효하지 않은 파라미터입니다: fieldName [rejectedValue]";
		assert expected.equals(result);
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
		assert expected.equals(result);
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
		String expected = "Invalid parameter: fieldName [rejectedValue], required type: requiredType";
		assert expected.equals(result);
	}
}
