package app.bottlenote.global.exception.util;

public class ExceptionUtil {

	public static String getErrorMessage(String fieldName, Object rejectedValue) {
		return "유효하지 않은 파라미터입니다: " + fieldName + " [" + rejectedValue + "]";
	}

	public static String getErrorMessage(String errorType, String fieldName, Object rejectedValue) {
		return errorType + ": " + fieldName + " [" + rejectedValue + "]";
	}

	public static String getErrorMessageForTypeMismatch(String fieldName, String rejectedValue, String requiredType) {
		return String.format("Invalid parameter: %s [%s], required type: %s", fieldName, rejectedValue, requiredType);
	}
}
