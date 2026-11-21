package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.fixture.AdminUserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 실제 HTTP로 admin 경계를 확인한다.
 *
 * <p>MockMvc는 sendError 뒤의 ERROR dispatch를 실행하지 않는다. admin 인증 실패는 sendError(403) 뒤 /error로 다시
 * 들어오므로, 이 요청이 product 체인으로 가면 401 JSON(product 형식)으로 바뀐다. 기존 admin 앱은 같은 체인이 다시 거부해 본문 없는 403으로
 * 끝났다.
 */
@Tag("integration")
@DisplayName("[integration] admin API 흡수 HTTP 경계")
class AdminApiHttpBoundaryIntegrationTest extends AdminApiIntegrationTestSupport {

  private final HttpClient client = HttpClient.newHttpClient();

  @LocalServerPort private int port;

  @Autowired private AdminUserTestFactory adminUserTestFactory;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Value("${security.jwt.secret-key}")
  private String secretKey;

  @Test
  @DisplayName("토큰 없이 인증 필수 admin API를 호출할 때 admin 앱처럼 본문 없는 403으로 끝난다")
  void 무토큰_admin_요청은_본문_없는_403() throws Exception {
    HttpResponse<String> response = get("/admin/api/v1/users", null);

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.body()).isEmpty();
    assertThat(response.headers().firstValue("Content-Type")).isEmpty();
  }

  @Test
  @DisplayName("일반 사용자 토큰으로 admin API를 호출할 때 product 형식(401 JSON)이 아니라 admin 앱의 403으로 끝난다")
  void 사용자_토큰_admin_요청은_본문_없는_403() throws Exception {
    HttpResponse<String> users = get("/admin/api/v1/users", getToken());
    HttpResponse<String> v2 = get("/admin/api/v2/curation-specs", getToken());

    assertThat(users.statusCode()).isEqualTo(403);
    assertThat(users.body()).isEmpty();
    assertThat(v2.statusCode()).isEqualTo(403);
    assertThat(v2.body()).isEmpty();
  }

  @Test
  @DisplayName("만료된 admin 토큰으로 admin API를 호출할 때 본문 없는 403으로 끝난다")
  void 만료_admin_토큰은_본문_없는_403() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();

    HttpResponse<String> response = get("/admin/api/v1/users", expiredAdminToken(admin));

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.body()).isEmpty();
  }

  @Test
  @DisplayName("admin 토큰으로 admin API를 호출할 때 정상 응답한다")
  void admin_토큰은_admin_API를_통과한다() throws Exception {
    HttpResponse<String> users = get("/admin/api/v1/users", adminToken());
    HttpResponse<String> v2 = get("/admin/api/v2/curation-specs", adminToken());

    assertThat(users.statusCode()).isEqualTo(200);
    assertThat(v2.statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("만료된 Bearer 헤더로 admin 공개 경로를 호출할 때 admin 앱처럼 JWT 필터를 건너뛰어 업무 응답을 준다")
  void 공개_admin_경로는_만료_토큰이_있어도_열린다() throws Exception {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();

    HttpResponse<String> response =
        post(
            "/admin/api/v1/auth/login",
            expiredAdminToken(admin),
            "{\"email\":\"" + admin.getEmail() + "\",\"password\":\"password123\"}");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(mapper.readTree(response.body()).at("/data/accessToken").asText()).isNotBlank();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/%61pi/v1/users",
        "/admin/api/%761/users",
        "/admin/api/v1/%75sers",
        "/admin/api/v1/users/"
      })
  @DisplayName("인코딩을 바꾼 admin 경로를 일반 사용자 토큰으로 호출할 때 product 체인으로 빠지지 않는다")
  void 인코딩_변형_admin_경로는_사용자_토큰으로_열리지_않는다(String path) throws Exception {
    HttpResponse<String> response = get(path, getToken());

    assertThat(response.statusCode()).isNotEqualTo(200).isNotEqualTo(401);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/admin/api/.env", "/admin/api/admin/login.php", "/admin/api/graphql"})
  @DisplayName("admin API 아래 스캐너 경로를 호출할 때 admin 앱처럼 403으로 막는다")
  void admin_API_아래_스캐너_경로는_403(String path) throws Exception {
    HttpResponse<String> response = get(path, adminToken());

    assertThat(response.statusCode()).isEqualTo(403);
    JsonNode body = mapper.readTree(response.body());
    assertThat(body.path("status").asInt()).isEqualTo(403);
    assertThat(body.path("path").asText()).isEqualTo(path);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/admin/login.php", "/admin", "/administrator/index.php", "/graphql"})
  @DisplayName("admin API 밖의 /admin 스캐너 경로를 호출할 때 product 체인이 계속 403으로 막는다")
  void admin_API_밖_스캐너_경로는_403(String path) throws Exception {
    assertThat(get(path, null).statusCode()).isEqualTo(403);
  }

  @Test
  @DisplayName("토큰 없이 product 인증 필수 API를 호출할 때 product 체인의 401 JSON을 유지한다")
  void product_무토큰_요청은_401_JSON() throws Exception {
    HttpResponse<String> response = get("/api/v1/business-support", null);

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(mapper.readTree(response.body()).path("success").asBoolean(true)).isFalse();
  }

  @Test
  @DisplayName("admin 토큰으로 product API를 호출할 때 기존 product 동작(인증 필수는 500, 공개는 200)을 유지한다")
  void admin_토큰_product_요청은_기존_동작() throws Exception {
    String token = adminToken();

    // admin 이메일이 users에 없으면 product JWT 필터에서 예외가 나 서버 오류로 끝나는 기존 동작이다
    assertThat(get("/api/v1/business-support", token).statusCode()).isEqualTo(500);
    assertThat(get("/api/v1/alcohols/categories", token).statusCode()).isEqualTo(200);
  }

  @Test
  @DisplayName("admin Origin의 preflight를 보낼 때 admin CORS 설정으로 허용하고 product Origin은 거부한다")
  void admin_CORS는_admin_Origin만_허용한다() throws Exception {
    HttpResponse<String> admin = preflight("/admin/api/v1/users", "https://admin.bottle-note.com");
    HttpResponse<String> product = preflight("/admin/api/v1/users", "https://bottle-note.com");

    assertThat(admin.statusCode()).isEqualTo(200);
    assertThat(admin.headers().firstValue("Access-Control-Allow-Origin"))
        .contains("https://admin.bottle-note.com");
    assertThat(product.statusCode()).isEqualTo(403);
    assertThat(product.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  @Test
  @DisplayName("admin 문서 경로를 호출할 때 토큰 없이 받을 수 있고 문서 전용 CORS를 쓴다")
  void admin_문서는_공개되고_문서_전용_CORS를_쓴다() throws Exception {
    HttpResponse<String> noOrigin = get(AdminApi.DOCS_PATH, null);
    HttpResponse<String> adminOrigin =
        send(
            HttpRequest.newBuilder(uri(AdminApi.DOCS_PATH))
                .header("Origin", "https://admin.bottle-note.com")
                .GET());
    HttpResponse<String> docsOrigin =
        send(
            HttpRequest.newBuilder(uri(AdminApi.DOCS_PATH))
                .header("Origin", "https://bottle-note.github.io")
                .GET());
    HttpResponse<String> yaml = get(AdminApi.DOCS_PATH + ".yaml", null);

    assertThat(noOrigin.statusCode()).isEqualTo(200);
    assertThat(mapper.readTree(noOrigin.body()).at("/servers/0/url").asText())
        .isEqualTo("http://localhost:" + port + "/admin/api");
    assertThat(adminOrigin.statusCode()).isEqualTo(403);
    assertThat(docsOrigin.statusCode()).isEqualTo(200);
    // admin 앱에서도 YAML 문서는 공개 경로가 아니었다
    assertThat(yaml.statusCode()).isEqualTo(403);
  }

  private String adminToken() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    return jwtTokenProvider
        .generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId())
        .accessToken();
  }

  private String expiredAdminToken(AdminUser admin) {
    String roles = String.join(",", admin.getRoles().stream().map(Enum::name).toList());
    return Jwts.builder()
        .setSubject(admin.getEmail())
        .claim("roles", roles)
        .claim("userId", admin.getId())
        .claim("isAdmin", true)
        .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
        .setExpiration(new Date(System.currentTimeMillis() - 60_000))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)), SignatureAlgorithm.HS512)
        .compact();
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return send(request);
  }

  private HttpResponse<String> post(String path, String token, String json) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return send(request);
  }

  private HttpResponse<String> preflight(String path, String origin) throws Exception {
    return send(
        HttpRequest.newBuilder(uri(path))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", origin)
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "authorization,content-type"));
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  // 인코딩한 경로를 그대로 보내야 하므로 URI 문자열을 직접 만든다
  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
