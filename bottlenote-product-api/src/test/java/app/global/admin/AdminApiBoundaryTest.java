package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.controller.AlcoholQueryController;
import app.bottlenote.alcohols.presentation.AdminAlcoholExcelExceptionHandler;
import app.bottlenote.alcohols.presentation.AdminAlcoholsController;
import app.bottlenote.curation.presentation.AdminCurationSpecController;
import app.bottlenote.curation.presentation.AdminSpecBasedCurationController;
import app.bottlenote.user.presentation.AdminUsersController;
import app.external.version.presentation.AppInfoController;
import app.global.admin.security.AdminApiRequestMatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@Tag("unit")
@DisplayName("[unit] admin API 흡수 경계 정의")
class AdminApiBoundaryTest {

  @ParameterizedTest
  @CsvSource({
    "/admin/api, true",
    "/admin/api/, true",
    "/admin/api/v1/users, true",
    "/admin/api/v2/curation-specs, true",
    "/admin/%61pi/v1/users, true",
    "/admin/api/%761/users, true",
    "/admin/apix, false",
    "/admin/login.php, false",
    "/Admin/api/v1/users, false",
    "/api/v1/reviews, false",
    "/error, false"
  })
  @DisplayName("admin API 경로를 판정할 때 MVC와 같은 디코딩 기준을 쓴다")
  void admin_API_경로_판정(String path, boolean expected) {
    assertThat(AdminApi.INSTANCE.isAdminApiPath(path)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "/admin/api/v1/auth/login, /v1/auth/login",
    "/admin/%61pi/v1/auth/login, /v1/auth/login",
    "/%61dmin/api/v1/access-control/ip-bans, /v1/access-control/ip-bans",
    "/admin/api/v1/%61uth/login, /v1/auth/login",
    "/admin/api/v1/users/, /v1/users/",
    "/admin/api, /",
    "/admin/api/, /",
    "/admin/apix/v1/users, ",
    "/api/v1/reviews, ",
    "/admin/api/v1/%zz, "
  })
  @DisplayName("mount 안쪽 경로를 구할 때 MVC처럼 세그먼트마다 디코딩하고 admin 경로가 아니면 null을 준다")
  void mount_안쪽_경로(String path, String expected) {
    assertThat(AdminApi.INSTANCE.pathWithinMount(path)).isEqualTo(expected);
  }

  @Test
  @DisplayName("일반 요청은 요청 경로로, ERROR dispatch는 원래 요청 URI로 admin 체인을 고른다")
  void admin_체인_선택은_ERROR_dispatch에서_원래_요청을_본다() {
    MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/admin/api/v1/users");
    MockHttpServletRequest adminError = errorDispatch("/admin/api/v1/users");
    MockHttpServletRequest productError = errorDispatch("/api/v1/reviews");
    MockHttpServletRequest unknownError = errorDispatch(null);

    assertThat(AdminApiRequestMatcher.INSTANCE.matches(adminRequest)).isTrue();
    assertThat(AdminApiRequestMatcher.INSTANCE.matches(adminError)).isTrue();
    assertThat(AdminApiRequestMatcher.INSTANCE.matches(productError)).isFalse();
    assertThat(AdminApiRequestMatcher.INSTANCE.matches(unknownError)).isFalse();
    assertThat(AdminApiRequestMatcher.INSTANCE.matches(new MockHttpServletRequest("GET", "/error")))
        .isFalse();
  }

  @Test
  @DisplayName("admin 컨트롤러를 판정할 때 이식한 패키지만 admin으로 보고 v2 컨트롤러를 구분한다")
  void admin_컨트롤러_판정() {
    assertThat(AdminApi.INSTANCE.isAdminController(AdminUsersController.class)).isTrue();
    assertThat(AdminApi.INSTANCE.isLegacyController(AdminAlcoholsController.class)).isTrue();
    assertThat(AdminApi.INSTANCE.isV2Controller(AdminCurationSpecController.class)).isTrue();
    assertThat(AdminApi.INSTANCE.isV2Controller(AdminSpecBasedCurationController.class)).isTrue();
    assertThat(AdminApi.INSTANCE.isLegacyController(AdminCurationSpecController.class)).isFalse();

    assertThat(AdminApi.INSTANCE.isAdminController(AlcoholQueryController.class)).isFalse();
    assertThat(AdminApi.INSTANCE.isAdminController(AppInfoController.class)).isFalse();
  }

  @Test
  @DisplayName("admin 엑셀 예외 처리기의 적용 범위는 admin 컨트롤러 패키지 목록과 같다")
  void 엑셀_예외_처리기_범위는_admin_컨트롤러_패키지와_같다() {
    RestControllerAdvice advice =
        AdminAlcoholExcelExceptionHandler.class.getAnnotation(RestControllerAdvice.class);

    assertThat(Set.of(advice.basePackages()))
        .isEqualTo(Set.copyOf(AdminApi.INSTANCE.getCONTROLLER_PACKAGES()));
    assertThat(advice.assignableTypes()).isEmpty();
    assertThat(advice.annotations()).isEmpty();
  }

  @Test
  @DisplayName("handler 결정 전에 admin 경로에서 업로드 크기 초과가 나면 admin 엑셀 처리기와 같은 응답을 쓴다")
  void handler_전_admin_multipart_크기_초과는_엑셀_처리기_응답() throws Exception {
    AdminMultipartFailureResolver resolver = resolver();
    MockHttpServletResponse response = new MockHttpServletResponse();

    var result =
        resolver.resolveException(
            new MockHttpServletRequest("POST", "/admin/api/v1/alcohols/excel/validate"),
            response,
            null,
            new MaxUploadSizeExceededException(-1));

    assertThat(result).isNotNull();
    assertThat(response.getStatus()).isEqualTo(400);
    JsonNode body = new ObjectMapper().readTree(response.getContentAsByteArray());
    assertThat(body.at("/errors/0/code").asText()).isEqualTo("EXCEL_FILE_TOO_LARGE");
  }

  @Test
  @DisplayName("product 경로나 handler가 정해진 요청의 multipart 예외는 처리하지 않고 기존 처리기에 넘긴다")
  void product_경로와_handler가_있는_요청은_처리하지_않는다() {
    AdminMultipartFailureResolver resolver = resolver();

    assertThat(
            resolver.resolveException(
                new MockHttpServletRequest("POST", "/api/v1/reviews"),
                new MockHttpServletResponse(),
                null,
                new MaxUploadSizeExceededException(-1)))
        .isNull();
    assertThat(
            resolver.resolveException(
                new MockHttpServletRequest("POST", "/admin/api/v1/alcohols/excel/validate"),
                new MockHttpServletResponse(),
                new Object(),
                new MultipartException("parse failure")))
        .isNull();
    assertThat(
            resolver.resolveException(
                new MockHttpServletRequest("POST", "/admin/api/v1/alcohols/excel/validate"),
                new MockHttpServletResponse(),
                null,
                new IllegalStateException("not multipart")))
        .isNull();
  }

  private static AdminMultipartFailureResolver resolver() {
    return new AdminMultipartFailureResolver(
        new AdminAlcoholExcelExceptionHandler(), new MappingJackson2HttpMessageConverter());
  }

  private static MockHttpServletRequest errorDispatch(String originalUri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
    request.setDispatcherType(DispatcherType.ERROR);
    if (originalUri != null) {
      request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, originalUri);
    }
    return request;
  }
}
