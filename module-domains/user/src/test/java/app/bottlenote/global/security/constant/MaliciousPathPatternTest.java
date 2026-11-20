package app.bottlenote.global.security.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.pattern.PathPatternParser;

@Tag("unit")
@DisplayName("MaliciousPathPattern 단위 테스트")
class MaliciousPathPatternTest {

  private static final String ADMIN_API = "/admin/api";

  @Test
  @DisplayName("getAllPatterns는 모든 악성 경로 패턴을 반환한다")
  void getAllPatterns_모든_패턴_반환() {
    // when
    String[] patterns = MaliciousPathPattern.getAllPatterns();

    // then
    assertThat(patterns).hasSize(MaliciousPathPattern.values().length);
    assertThat(patterns).contains("/.env*", "/.git/**", "/wp-admin/**", "/phpmyadmin/**");
  }

  @Test
  @DisplayName("getAllPatterns는 빈 배열이 아니다")
  void getAllPatterns_비어있지_않음() {
    // when
    String[] patterns = MaliciousPathPattern.getAllPatterns();

    // then
    assertThat(patterns).isNotEmpty();
  }

  @Test
  @DisplayName("getPatternsContaining은 키워드를 포함하는 패턴만 반환한다")
  void getPatternsContaining_키워드_필터링() {
    // when
    String[] wpPatterns = MaliciousPathPattern.getPatternsContaining("WordPress");

    // then
    assertThat(wpPatterns).contains("/wp-admin/**", "/wp-login.php", "/wp-config.php");
    assertThat(wpPatterns).doesNotContain("/.env*", "/.git/**");
  }

  @Test
  @DisplayName("getPatternsContaining은 패턴 문자열로도 필터링할 수 있다")
  void getPatternsContaining_패턴_문자열_필터링() {
    // when
    String[] envPatterns = MaliciousPathPattern.getPatternsContaining(".env");

    // then
    assertThat(envPatterns).contains("/.env*", "/.env.local", "/.env.production", "/.env.backup");
  }

  @Test
  @DisplayName("모든 패턴은 슬래시로 시작한다")
  void 모든_패턴_슬래시로_시작() {
    // when & then
    for (MaliciousPathPattern pattern : MaliciousPathPattern.values()) {
      assertThat(pattern.getPattern()).as("패턴 '%s'는 슬래시로 시작해야 한다", pattern.name()).startsWith("/");
    }
  }

  @Test
  @DisplayName("모든 패턴은 설명을 가진다")
  void 모든_패턴_설명_존재() {
    // when & then
    for (MaliciousPathPattern pattern : MaliciousPathPattern.values()) {
      assertThat(pattern.getDescription()).as("패턴 '%s'는 설명이 있어야 한다", pattern.name()).isNotBlank();
    }
  }

  @Test
  @DisplayName("주요 보안 취약점 경로가 포함되어 있다")
  void 주요_보안_취약점_경로_포함() {
    // when
    String[] patterns = MaliciousPathPattern.getAllPatterns();

    // then - 환경 설정 파일
    assertThat(patterns).contains("/.env*");

    // then - 버전 관리
    assertThat(patterns).contains("/.git/**");

    // then - WordPress
    assertThat(patterns).contains("/wp-admin/**", "/wp-login.php");

    // then - DB 관리 도구
    assertThat(patterns).contains("/phpmyadmin/**");

    // then - SSL/인증서
    assertThat(patterns).contains("/.well-known/**");
  }

  @Test
  @DisplayName("HTTP 요청 경로가 악성 패턴에 해당하는지 판별한다")
  void matches_악성_요청_경로_판별() {
    assertThat(MaliciousPathPattern.matches(new MockHttpServletRequest("GET", "/.env"))).isTrue();
    assertThat(MaliciousPathPattern.matches(new MockHttpServletRequest("GET", "/.git/config")))
        .isTrue();
    assertThat(MaliciousPathPattern.matches(new MockHttpServletRequest("GET", "/api/v1/regions")))
        .isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin",
        "/admin/",
        "/admin/login.php",
        "/admin/index.html",
        "/admin/api/v1/users",
        "/administrator/index.php"
      })
  @DisplayName("전체 경로 기준 판정은 /admin 아래 경로를 계속 스캐너 경로로 막는다")
  void matches_전체_경로_기준_관리자_경로_차단(String path) {
    assertThat(MaliciousPathPattern.matches(new MockHttpServletRequest("GET", path))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/api",
        "/admin/api/",
        "/admin/api/v1/users",
        "/admin/api/v2/curation-specs",
        "/admin/api/v1/openapi.admin.json",
        "/admin/api/%761/users",
        "/admin/%61pi/v1/users"
      })
  @DisplayName("mount 기준 판정은 mount 아래의 관리자 API 경로를 막지 않는다")
  void matchesUnder_관리자_API_허용(String path) {
    assertThat(
            MaliciousPathPattern.matchesUnder(new MockHttpServletRequest("GET", path), ADMIN_API))
        .isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/api/.env",
        "/admin/api/.git/config",
        "/admin/api/admin/login.php",
        "/admin/api/admin/api/v1/users",
        "/admin/api/graphql",
        "/admin/api/wp-admin/x"
      })
  @DisplayName("mount 기준 판정은 mount 안쪽의 스캐너 경로를 context-path 앱처럼 막는다")
  void matchesUnder_mount_안쪽_스캐너_경로_차단(String path) {
    assertThat(
            MaliciousPathPattern.matchesUnder(new MockHttpServletRequest("GET", path), ADMIN_API))
        .isTrue();
  }

  @ParameterizedTest
  @CsvSource({"/error, false", "/admin/login.php, true", "/admin, true", "/.env, true"})
  @DisplayName("mount 밖의 경로는 전체 경로 기준 판정과 같다")
  void matchesUnder_mount_밖은_전체_경로_기준(String path, boolean expected) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

    assertThat(MaliciousPathPattern.matchesUnder(request, ADMIN_API)).isEqualTo(expected);
    assertThat(MaliciousPathPattern.matches(request)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/v1/users",
        "/v2/curation-specs",
        "/v1/openapi.admin.json",
        "/",
        "/.env",
        "/.git/config",
        "/admin/login.php",
        "/admin/api/v1/users",
        "/graphql",
        "/graphiql/x",
        "/backup/dump",
        "/db.sql"
      })
  @DisplayName("context-path 없이 /admin/api 아래에 둔 판정은 context-path가 /admin/api인 기존 admin 앱 판정과 같다")
  void matchesUnder_기존_admin_앱_판정과_같다(String innerPath) {
    MockHttpServletRequest adminApp = new MockHttpServletRequest("GET", ADMIN_API + innerPath);
    adminApp.setContextPath(ADMIN_API);
    MockHttpServletRequest merged = new MockHttpServletRequest("GET", ADMIN_API + innerPath);

    assertThat(MaliciousPathPattern.matchesUnder(merged, ADMIN_API))
        .isEqualTo(MaliciousPathPattern.matches(adminApp));
  }

  @ParameterizedTest
  @CsvSource({
    "/admin/api/v1/users, false",
    "/admin/api/admin/login.php, true",
    "/admin/api/admin/api/v1/users, true",
    "/admin/api/.env, true",
    "/admin/api/graphql, true"
  })
  @DisplayName("context-path가 /admin/api인 기존 admin 앱의 전체 경로 기준 판정은 바뀌지 않는다")
  void matches_admin_앱_context_path_판정_유지(String requestUri, boolean expected) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
    request.setContextPath(ADMIN_API);

    assertThat(MaliciousPathPattern.matches(request)).isEqualTo(expected);
  }

  @Test
  @DisplayName("모든 패턴은 Spring Security PathPattern으로 파싱 가능해야 한다")
  void 모든_패턴_PathPattern_호환() {
    // given
    PathPatternParser parser = new PathPatternParser();

    // when & then
    for (MaliciousPathPattern pattern : MaliciousPathPattern.values()) {
      assertThatCode(() -> parser.parse(pattern.getPattern()))
          .as("패턴 '%s' (%s)는 PathPattern으로 파싱 가능해야 한다", pattern.name(), pattern.getPattern())
          .doesNotThrowAnyException();
    }
  }
}
