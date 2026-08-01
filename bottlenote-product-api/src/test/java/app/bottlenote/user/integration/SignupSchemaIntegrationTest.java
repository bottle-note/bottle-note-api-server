package app.bottlenote.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("integration")
@DisplayName("[integration] 가입 대기 스키마")
class SignupSchemaIntegrationTest extends IntegrationTestSupport {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("가입 토큰과 사용자 동의 이력 테이블이 존재한다")
  void signup_tables_exist() {
    // when
    Integer tableCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name IN ('signup_tokens', 'user_agreements')
            """,
            Integer.class);

    // then
    assertThat(tableCount).isEqualTo(2);
  }
}
