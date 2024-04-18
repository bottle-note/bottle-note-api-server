package app.bottlenote.global.exception.util;

public class ExceptionUtil {

	/**
	 * 파라미터 유효성 검증 실패 시 에러 메시지 반환
	 *
	 * @param rejectedValue  the rejected value
	 * @param defaultMessage 기본 에러 메시지  @NotNull(message = "필수 값입니다.") 등의 메시지
	 * @return the error message
	 */
	public static String getErrorMessage(Object rejectedValue, String defaultMessage) {
		return defaultMessage != null ? defaultMessage : "유효하지 않은 파라미터입니다: " + " (" + rejectedValue + ")";
	}

	public static String getErrorMessage(String errorType, String fieldName, Object rejectedValue) {
		return errorType + ": " + fieldName + " [" + rejectedValue + "]";
	}

	public static String getErrorMessageForTypeMismatch(String fieldName, String rejectedValue, String requiredType) {
//		return String.format("Invalid parameter: %s [%s], required type: %s", fieldName, rejectedValue, requiredType);
		return String.format("'%s' 필드는 '%s' 타입이 필요하지만, 잘못된 값 '%s'이(가) 제공되었습니다.", fieldName, requiredType, rejectedValue);
	}
}
