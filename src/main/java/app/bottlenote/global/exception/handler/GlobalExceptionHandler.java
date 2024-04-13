package app.bottlenote.global.exception.handler;

import app.bottlenote.global.data.response.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	/**
	 * 하위 타입에 속하지 않은 모든 예외에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(value = {Exception.class})
	public ResponseEntity<?> handleGenericException(Exception exception) {
		Map<String, String> message = Map.of("message", exception.getMessage());
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
	public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException exception) {
		BindingResult bindingResult = exception.getBindingResult();
		List<FieldError> fieldErrors = bindingResult.getFieldErrors();

		Map<String, String> errorMessages = new HashMap<>();

		for (FieldError fieldError : fieldErrors) {
			String fieldName = fieldError.getField();
			Object rejectedValue = fieldError.getRejectedValue();
			String defaultMessage = fieldError.getDefaultMessage();
			String errorMessage = getErrorMessage(rejectedValue,defaultMessage);
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
	public ResponseEntity<?> handleTypeMismatchException(MethodArgumentTypeMismatchException exception) {

		String fieldName = exception.getName();
		String rejectedValue = Objects.requireNonNull(exception.getValue()).toString();
		String requiredType = Objects.requireNonNull(exception.getRequiredType()).getSimpleName();

		String errorMessage = getErrorMessageForTypeMismatch(fieldName, rejectedValue, requiredType);

		Map<String, String> message = Map.of("message", errorMessage);

		return new ResponseEntity<>(GlobalResponse.error(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
	}

}
