package app.bottlenote.global.exception.handler;

import app.bottlenote.global.data.response.GlobalResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessage;
import static app.bottlenote.global.exception.util.ExceptionUtil.getErrorMessageForTypeMismatch;

@Slf4j(topic = "GlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String KEY_MESSAGE = "message";

	/**
	 * 하위 타입에 속하지 않은 모든 예외에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(value = {Exception.class})
	public ResponseEntity<GlobalResponse> handleGenericException(Exception exception) {
		Map<String, String> message = Map.of(KEY_MESSAGE, exception.getMessage());
		GlobalResponse error = GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), message);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}


	/**
	 * Valid 어노테이션을 사용하여 검증에 실패한 경우에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<GlobalResponse> handleValidationException(MethodArgumentNotValidException exception) {
		BindingResult bindingResult = exception.getBindingResult();
		List<FieldError> fieldErrors = bindingResult.getFieldErrors();

		Map<String, String> errorMessages = new HashMap<>();

		for (FieldError fieldError : fieldErrors) {
			String fieldName = fieldError.getField();
			Object rejectedValue = fieldError.getRejectedValue();
			String defaultMessage = fieldError.getDefaultMessage();
			String errorMessage = getErrorMessage(rejectedValue, defaultMessage);
			errorMessages.put(fieldName, errorMessage);
		}

		GlobalResponse error = GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), errorMessages);
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	/**
	 * 파라미터 타입이 일치하지 않는 경우에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<GlobalResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException exception) {

		String fieldName = exception.getName();
		String rejectedValue = Objects.requireNonNull(exception.getValue()).toString();
		String requiredType = Objects.requireNonNull(exception.getRequiredType()).getSimpleName();

		String errorMessage = getErrorMessageForTypeMismatch(fieldName, rejectedValue, requiredType);

		Map<String, String> message = Map.of(KEY_MESSAGE, errorMessage);

		return new ResponseEntity<>(GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
	}

	/**
	 * JSON 파싱에 실패한 경우에 대한 처리
	 * 잘못된 타입 발견 시 바로 HttpMessageNotReadableException가 반환되기 떄문에 개별적인 오류 메시지만 반환된다.
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<GlobalResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
		String finallyErrorMessage = "요청 본문을 파싱하는 도중 오류가 발생했습니다. 요청 본문의 형식을 확인해주세요.";
		Throwable cause = exception.getCause();

		if (cause instanceof JsonMappingException jsonMappingException) {
			List<JsonMappingException.Reference> path = jsonMappingException.getPath();
			if (!path.isEmpty()) {
				JsonMappingException.Reference lastReference = path.get(path.size() - 1);
				String fieldName = lastReference.getFieldName();
				String errorDetailMessage = String.format("'%s' 필드의 값이 잘못되었습니다. 해당 필드의 값의 타입을 확인해주세요.", fieldName);
				Map<String, String> message = Map.of(KEY_MESSAGE, errorDetailMessage);
				return new ResponseEntity<>(GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
			}
		}

		Map<String, String> message = Map.of(KEY_MESSAGE, finallyErrorMessage);
		return new ResponseEntity<>(GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
	}
}
