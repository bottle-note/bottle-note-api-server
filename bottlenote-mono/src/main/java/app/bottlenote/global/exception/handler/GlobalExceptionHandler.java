package app.bottlenote.global.exception.handler;

import static app.bottlenote.global.exception.custom.code.ValidExceptionCode.UNKNOWN_ERROR;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.AbstractCustomException;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.global.security.jwt.JwtExceptionType;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;

@Slf4j(topic = "GlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 사용자 정의 예외에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(AbstractCustomException.class)
  public ResponseEntity<GlobalResponse> handleCustomException(AbstractCustomException exception) {
    log.warn("사용자 정의 예외 발생 : {}", exception.getMessage());
    return GlobalResponse.error(exception);
  }

  /**
   * 하위 타입에 속하지 않은 모든 예외에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(value = {Exception.class})
  public ResponseEntity<GlobalResponse> handleGenericException(Exception exception) {
    log.error("Exception.class 예외 발생 : {}", exception.getMessage());
    Error error = Error.of(UNKNOWN_ERROR.message(exception.getMessage()));
    return GlobalResponse.error(error);
  }

  /**
   * Valid 어노테이션을 사용하여 검증에 실패한 경우에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GlobalResponse> handleValidationException(
      MethodArgumentNotValidException exception) {

    BindingResult bindingResult = exception.getBindingResult();

    List<FieldError> fieldErrors = bindingResult.getFieldErrors();

    Set<Error> errorSet = new HashSet<>();

    for (FieldError fieldError : fieldErrors) {
      // String fieldName = fieldError.getField();  // 필드명
      // Object rejectedValue = fieldError.getRejectedValue(); // 거부된 값

      ValidExceptionCode code =
          ValidExceptionCode.valueOf(fieldError.getDefaultMessage()); // ALCOHOL_ID_REQUIRED

      Error error = Error.of(code);

      errorSet.add(error);
    }

    return GlobalResponse.error(errorSet);
  }

  /**
   * 파라미터 타입이 일치하지 않는 경우에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<GlobalResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException exception) {
    String fieldName = exception.getName();
    String rejectedValue = Objects.requireNonNull(exception.getValue()).toString();
    String requiredType = Objects.requireNonNull(exception.getRequiredType()).getSimpleName();
    String errorMessage =
        String.format(
            "'%s' 필드는 '%s' 타입이 필요하지만, 잘못된 값 '%s'가 입력되었습니다.",
            fieldName, requiredType, rejectedValue);

    ValidExceptionCode code = ValidExceptionCode.TYPE_MISMATCH;
    code.message(errorMessage);
    return GlobalResponse.error(Error.of(code));
  }

  /**
   * 필수 요청 파라미터가 누락된 경우에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<GlobalResponse> handleMissingRequestParameterException(
      MissingServletRequestParameterException exception) {
    String parameterName = exception.getParameterName();
    String parameterType = exception.getParameterType();
    String errorMessage =
        String.format(
            "'%s' 필드는 '%s' 타입의 필수 파라미터이지만, 요청에 포함되지 않았습니다.", parameterName, parameterType);

    log.warn("필수 요청 파라미터 누락 : {}", errorMessage);

    ValidExceptionCode code = ValidExceptionCode.REQUIRED_PARAMETER_MISSING;
    code.message(errorMessage);
    return GlobalResponse.error(Error.of(code));
  }

  /**
   * 매핑되지 않은 경로를 요청한 경우에 대한 처리<br>
   * 서버 장애가 아니라 클라이언트의 잘못된 요청이므로 404로 응답한다.
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<GlobalResponse> handleNoResourceFoundException(
      NoResourceFoundException exception) {
    String httpMethod = exception.getHttpMethod().name();
    String resourcePath = exception.getResourcePath();
    String errorMessage =
        String.format("'%s /%s' 요청에 매핑된 API가 존재하지 않습니다. 요청 경로를 확인해주세요.", httpMethod, resourcePath);

    log.warn("매핑되지 않은 경로 요청 : {}", errorMessage);

    ValidExceptionCode code = ValidExceptionCode.RESOURCE_NOT_FOUND;
    code.message(errorMessage);
    return GlobalResponse.error(Error.of(code));
  }

  /**
   * JSON 파싱에 실패한 경우에 대한 처리<br>
   * 잘못된 타입 발견 시 바로 HttpMessageNotReadableException가 <br>
   * 반환되기 떄문에 개별적인 오류 메시지만 반환된다. <br>
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<GlobalResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception) {
    final String finallyErrorMessage = "요청 본문을 파싱하는 도중 오류가 발생했습니다. 요청 본문의 형식을 확인해주세요.";

    Throwable cause = exception.getCause();

    if (cause instanceof JsonMappingException jsonMappingException) {
      List<JsonMappingException.Reference> path = jsonMappingException.getPath();

      if (!path.isEmpty()) {
        JsonMappingException.Reference lastReference = path.get(path.size() - 1);
        String fieldName = lastReference.getFieldName();
        String errorDetailMessage =
            String.format(
                "'%s' 필드의 값이 잘못되었습니다. 해당 필드의 값의 타입을 확인해주세요.: '%s'",
                fieldName, getRefinedCauseMessage(cause.getMessage()));

        Error error = Error.of(ValidExceptionCode.JSON_PASSING_FAILED.message(errorDetailMessage));
        return GlobalResponse.error(error);
      }
    }

    Error error = Error.of(ValidExceptionCode.JSON_PASSING_FAILED.message(finallyErrorMessage));

    return GlobalResponse.error(error);
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
  @ExceptionHandler(
      value = {
        SignatureException.class,
        MalformedJwtException.class,
        ExpiredJwtException.class,
        UnsupportedJwtException.class,
        IllegalArgumentException.class
      })
  public ResponseEntity<GlobalResponse> jwtTokenException(Exception e) {

    JwtExceptionType exceptionType = getJwtExceptionType(e);

    log.info(" jwtTokenException 관련 예외 발생 : {}", exceptionType);

    return GlobalResponse.error(Error.of(exceptionType));
  }

  /**
   * JwtExceptionType을 반환하는 메서드입니다.
   *
   * @param e Exception
   * @return JwtExceptionType
   */
  private JwtExceptionType getJwtExceptionType(Exception e) {
    if (e instanceof SignatureException) {
      return JwtExceptionType.INVALID_SIGNATURE;
    } else if (e instanceof MalformedJwtException) {
      return JwtExceptionType.MALFORMED_TOKEN;
    } else if (e instanceof ExpiredJwtException) {
      return JwtExceptionType.EXPIRED_TOKEN;
    } else if (e instanceof UnsupportedJwtException) {
      return JwtExceptionType.UNSUPPORTED_TOKEN;
    } else if (e instanceof IllegalArgumentException) {
      return JwtExceptionType.ILLEGAL_ARGUMENT;
    } else {
      return JwtExceptionType.UNKNOWN_ERROR;
    }
  }

  /**
   * AWS 관련 예외에 대한 처리
   *
   * @param exception the exception
   * @return the response entity
   */
  @ExceptionHandler(SdkException.class)
  public ResponseEntity<GlobalResponse> handleSdkException(SdkException exception) {
    String errorMessage;

    if (exception instanceof AwsServiceException ase) {
      errorMessage = "AWS 서비스 오류가 발생했습니다: " + ase.getMessage();
    } else if (exception instanceof SdkClientException sce) {
      errorMessage = "AWS SDK 오류가 발생했습니다: " + sce.getMessage();
    } else {
      errorMessage = "AWS 클라이언트 오류가 발생했습니다: " + exception.getMessage();
    }

    Error error = Error.of(ValidExceptionCode.AWS_ERROR.message(errorMessage));
    return GlobalResponse.error(error);
  }
}
