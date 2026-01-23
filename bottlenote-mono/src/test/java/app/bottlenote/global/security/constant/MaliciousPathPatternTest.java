package app.bottlenote.global.security.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.pattern.PathPatternParser;

@Tag("unit")
@DisplayName("MaliciousPathPattern 단위 테스트")
class MaliciousPathPatternTest {

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
