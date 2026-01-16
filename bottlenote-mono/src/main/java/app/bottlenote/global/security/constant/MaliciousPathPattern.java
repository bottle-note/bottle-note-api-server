package app.bottlenote.global.security.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 악성 봇/스캐너가 자주 탐색하는 경로 패턴 목록. Spring Security에서 denyAll() 처리에 사용. */
@Getter
@RequiredArgsConstructor
public enum MaliciousPathPattern {

  // 환경 설정 파일
  ENV_FILE("/.env*", "환경 변수 파일"),
  ENV_LOCAL("/.env.local", "로컬 환경 변수"),
  ENV_PRODUCTION("/.env.production", "프로덕션 환경 변수"),
  ENV_BACKUP("/.env.backup", "환경 변수 백업"),

  // 버전 관리
  GIT_DIRECTORY("/.git/**", "Git 저장소"),
  GITIGNORE("/.gitignore", "Git ignore 파일"),
  SVN_DIRECTORY("/.svn/**", "SVN 저장소"),

  // WordPress
  WP_ADMIN("/wp-admin/**", "WordPress 관리자"),
  WP_LOGIN("/wp-login.php", "WordPress 로그인"),
  WP_CONFIG("/wp-config.php", "WordPress 설정"),
  WP_INCLUDES("/wp-includes/**", "WordPress 인클루드"),
  WP_CONTENT("/wp-content/**", "WordPress 컨텐츠"),
  XMLRPC("/xmlrpc.php", "XML-RPC 엔드포인트"),

  // PHP/DB 관리 도구
  PHP_MY_ADMIN("/phpmyadmin/**", "phpMyAdmin"),
  PMA("/pma/**", "phpMyAdmin 별칭"),
  MYSQL_ADMIN("/mysql/**", "MySQL 관리"),
  ADMINER("/adminer.php", "Adminer DB 관리"),
  PHP_INFO("/phpinfo.php", "PHP 정보"),
  INFO_PHP("/info.php", "서버 정보"),

  // 서버 상태
  SERVER_STATUS("/server-status", "Apache 서버 상태"),
  SERVER_INFO("/server-info", "Apache 서버 정보"),

  // 백업/덤프 파일 (루트 경로만 차단, ** 뒤에 패턴 불가)
  SQL_FILE("/*.sql", "루트 SQL 덤프 파일"),
  BAK_FILE("/*.bak", "루트 백업 파일"),
  BACKUP_FILE("/*.backup", "루트 백업 파일"),
  DUMP_FILE("/*.dump", "루트 덤프 파일"),
  DB_DUMP("/db.sql", "DB 덤프 파일"),
  DATABASE_DUMP("/database.sql", "DB 덤프 파일"),
  BACKUP_DIR("/backup/**", "백업 디렉토리"),
  BACKUPS_DIR("/backups/**", "백업 디렉토리"),

  // SSL/인증서 관련
  WELL_KNOWN("/.well-known/**", "ACME 챌린지/인증"),

  // 기타 스캔 대상
  DS_STORE("/.DS_Store", "macOS 메타데이터"),
  VSCODE("/.vscode/**", "VSCode 설정"),
  IDEA("/.idea/**", "IntelliJ 설정"),
  AWS_CREDENTIALS("/aws/credentials", "AWS 자격증명"),
  DOCKER_COMPOSE("/docker-compose.yml", "Docker Compose"),
  CGI_BIN("/cgi-bin/**", "CGI 스크립트"),

  // 관리자 경로 (일반적인 스캔 대상)
  ADMIN("/admin/**", "관리자 페이지"),
  ADMINISTRATOR("/administrator/**", "관리자 페이지");

  private final String pattern;
  private final String description;

  /** 모든 악성 경로 패턴 배열 반환 */
  public static String[] getAllPatterns() {
    return Arrays.stream(values()).map(MaliciousPathPattern::getPattern).toArray(String[]::new);
  }

  /** 특정 카테고리의 패턴만 반환 (패턴 문자열 포함 여부로 필터링) */
  public static String[] getPatternsContaining(String keyword) {
    return Arrays.stream(values())
        .filter(p -> p.pattern.contains(keyword) || p.description.contains(keyword))
        .map(MaliciousPathPattern::getPattern)
        .toArray(String[]::new);
  }
}
