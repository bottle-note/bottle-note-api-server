package app.bottlenote.global.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    assertThat(error.message()).contains("GET", "v1/mfds/nope");
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

  @SuppressWarnings("unchecked")
  private Error firstError(ResponseEntity<GlobalResponse> response) {
    GlobalResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getSuccess()).isFalse();
    return ((List<Error>) body.getErrors()).get(0);
  }
}
