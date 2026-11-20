package app.bottlenote.global.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Tag("unit")
@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("매핑되지 않은 경로를 요청하면 404와 RESOURCE_NOT_FOUND를 반환한다")
  void 매핑되지_않은_경로는_404를_반환한다() {
    NoResourceFoundException exception =
        new NoResourceFoundException(HttpMethod.GET, "v1/mfds/nope");

    ResponseEntity<GlobalResponse> response = handler.handleNoResourceFoundException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    Error error = firstError(response);
    assertThat(error.code()).isEqualTo(ValidExceptionCode.RESOURCE_NOT_FOUND);
    assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND);
    // 이 응답은 인증 이전에도 나가므로 요청 경로나 프레임워크 내부 표현이 본문에 섞이면 안 된다.
    assertThat(error.message()).doesNotContain("v1/mfds/nope", "static resource");
  }

  @Test
  @DisplayName("필수 요청 파라미터가 누락되면 400과 REQUIRED_PARAMETER_MISSING을 반환한다")
  void 필수_요청_파라미터_누락은_400을_반환한다() {
    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("ip", "String");

    ResponseEntity<GlobalResponse> response =
        handler.handleMissingRequestParameterException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Error error = firstError(response);
    assertThat(error.code()).isEqualTo(ValidExceptionCode.REQUIRED_PARAMETER_MISSING);
    assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(error.message()).contains("ip", "String");
  }

  @Test
  @DisplayName("검증 메시지가 ValidExceptionCode 이름일 때 그 코드로 응답한다")
  void 검증_메시지가_코드_이름이면_그_코드로_응답한다() {
    ResponseEntity<GlobalResponse> response =
        handler.handleValidationException(
            validationFailure(
                new FieldError(
                    "request",
                    "size",
                    101,
                    false,
                    new String[] {"Max"},
                    null,
                    "ADMIN_REVIEW_SIZE_MAXIMUM")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Error error = firstError(response);
    assertThat(error.code()).isEqualTo(ValidExceptionCode.ADMIN_REVIEW_SIZE_MAXIMUM);
    assertThat(error.message())
        .isEqualTo(ValidExceptionCode.ADMIN_REVIEW_SIZE_MAXIMUM.getMessage());
  }

  @Test
  @DisplayName("검증 메시지가 코드가 아닌 문구일 때 INVALID_REQUEST_VALUE에 그 문구를 담아 응답한다")
  void 검증_메시지가_일반_문구면_요청_값_오류로_응답한다() {
    ResponseEntity<GlobalResponse> response =
        handler.handleValidationException(
            validationFailure(
                new FieldError(
                    "request",
                    "korName",
                    "",
                    false,
                    new String[] {"NotBlank"},
                    null,
                    "한글 이름은 필수입니다.")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Error error = firstError(response);
    assertThat(error.code()).isEqualTo(ValidExceptionCode.INVALID_REQUEST_VALUE);
    assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(error.message()).isEqualTo("한글 이름은 필수입니다.");
  }

  @Test
  @DisplayName("요청 값을 필드 타입으로 바꾸지 못할 때 TYPE_MISMATCH와 필드 이름으로 응답한다")
  void 값_변환_실패는_타입_오류로_응답한다() {
    ResponseEntity<GlobalResponse> response =
        handler.handleValidationException(
            validationFailure(
                new FieldError(
                    "request",
                    "size",
                    "abc",
                    true,
                    new String[] {"typeMismatch"},
                    null,
                    "Failed to convert property value of type 'java.lang.String'")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    Error error = firstError(response);
    assertThat(error.code()).isEqualTo(ValidExceptionCode.TYPE_MISMATCH);
    assertThat(error.message()).contains("size").doesNotContain("java.lang.String");
  }

  private MethodArgumentNotValidException validationFailure(FieldError fieldError) {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(fieldError);
    try {
      MethodParameter parameter =
          new MethodParameter(
              GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", Object.class),
              0);
      return new MethodArgumentNotValidException(parameter, bindingResult);
    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("unused")
  private void validationTarget(Object request) {}

  @SuppressWarnings("unchecked")
  private Error firstError(ResponseEntity<GlobalResponse> response) {
    GlobalResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getSuccess()).isFalse();
    return ((List<Error>) body.getErrors()).get(0);
  }
}
