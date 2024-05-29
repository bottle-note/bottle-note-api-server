package app.bottlenote.global.exception.handler;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.AbstractCustomException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j(topic = "GlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String KEY_MESSAGE = "message";

	private ResponseEntity<GlobalResponse> createResponseEntity(Exception exception, HttpStatus status, Map<String, String> message) {
		log.warn("예외 발생 : ", exception);
		return new ResponseEntity<>(GlobalResponse.error(status.value(), message), status);
	}


	/**
	 * 사용자 정의 예외에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(AbstractCustomException.class)
	public ResponseEntity<GlobalResponse> handleCustomException(AbstractCustomException exception) {
		String errorMessage = exception.getMessage();
		Map<String, String> message = Map.of(KEY_MESSAGE, errorMessage);
		return createResponseEntity(exception, HttpStatus.BAD_REQUEST, message);
	}

	/**
	 * 하위 타입에 속하지 않은 모든 예외에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(value = {Exception.class})
	public ResponseEntity<GlobalResponse> handleGenericException(Exception exception) {
		Map<String, String> message = Map.of(KEY_MESSAGE, exception.getMessage());
		return createResponseEntity(exception, HttpStatus.BAD_REQUEST, message);
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
			String defaultMessage = fieldError.getDefaultMessage();   // 한글로 오류 메시지 생성
			String errorMessage = String.format("필드 '%s'의 값 '%s'가 유효하지 않습니다: %s", fieldName, rejectedValue, defaultMessage);
			errorMessages.put(fieldName, errorMessage);
		}

		return createResponseEntity(exception, HttpStatus.BAD_REQUEST, errorMessages);
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

		String errorMessage = String.format("'%s' 필드는 '%s' 타입이 필요하지만, 잘못된 값 '%s'가 입력되었습니다.",
			fieldName, requiredType, rejectedValue);

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
				String errorDetailMessage = String.format("'%s' 필드의 값이 잘못되었습니다. 해당 필드의 값의 타입을 확인해주세요.: '%s'", fieldName, getRefinedCauseMessage(cause.getMessage()));
				Map<String, String> message = Map.of(KEY_MESSAGE, errorDetailMessage);
				return createResponseEntity(exception, HttpStatus.BAD_REQUEST, message);
			}
		}

		Map<String, String> message = Map.of(KEY_MESSAGE, finallyErrorMessage);
		return createResponseEntity(exception, HttpStatus.BAD_REQUEST, message);
	}

	/**
	 * 원인 메시지를 더욱 정제된 형태로 반환
	 *
	 * @param causeMessage the cause message
	 * @return the refined cause message
	 */
	private String getRefinedCauseMessage(String causeMessage) {
		if (causeMessage.contains("problem")) {
			return causeMessage.substring(causeMessage.indexOf("problem"), causeMessage.indexOf("\n"));
		}
		return "";
	}


	/**
	 * JWT 토큰 관련 통합 예외 처리
	 *
	 * @return the response entity
	 */
	@ExceptionHandler(value = {SignatureException.class, MalformedJwtException.class, ExpiredJwtException.class})
	public ResponseEntity<GlobalResponse> jwtTokenException() {
		log.warn("jwt 토큰 예외 발생 : {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		GlobalResponse fail = GlobalResponse.fail(403, "올바르지 않은 토큰입니다.");
		return ResponseEntity
			.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
			.body(fail);
	}

	/**
	 * AWS 관련 예외에 대한 처리
	 *
	 * @param exception the exception
	 * @return the response entity
	 */
	@ExceptionHandler(AmazonClientException.class)
	public ResponseEntity<GlobalResponse> handleAmazonClientException(AmazonClientException exception) {
		String errorMessage;
		HttpStatus status;

		if (exception instanceof AmazonServiceException ase) {
			errorMessage = "AWS 서비스 오류가 발생했습니다: " + ase.getMessage();
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		} else if (exception instanceof SdkClientException sce) {
			errorMessage = "AWS SDK 오류가 발생했습니다: " + sce.getMessage();
			status = HttpStatus.SERVICE_UNAVAILABLE;
		} else {
			errorMessage = "AWS 클라이언트 오류가 발생했습니다: " + exception.getMessage();
			status = HttpStatus.SERVICE_UNAVAILABLE;
		}

		return createResponseEntity(exception, status, Map.of(KEY_MESSAGE, errorMessage));
	}
}
