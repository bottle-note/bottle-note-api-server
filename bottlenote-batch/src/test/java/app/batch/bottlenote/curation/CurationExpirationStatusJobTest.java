package app.batch.bottlenote.curation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentMatcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("batch")
@DisplayName("[batch] Curation 만료 상태 동기화 Job")
class CurationExpirationStatusJobTest {

  @Test
  @DisplayName("노출 종료일이 지난 활성 큐레이션을 native SQL 한 번으로 비활성화한다")
  void executeInternal_deactivatesExpiredActiveCurationsWithNativeSql() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.update(argThat(nativeExpirationUpdateSql()))).thenReturn(3);
    CurationExpirationStatusJob job = new CurationExpirationStatusJob(jdbcTemplate);

    assertThatCode(() -> job.executeInternal(null)).doesNotThrowAnyException();

    verify(jdbcTemplate).update(argThat(nativeExpirationUpdateSql()));
  }

  @Test
  @DisplayName("만료 대상이 없어도 조용히 종료한다")
  void executeInternal_whenNoExpiredCurations_finishesQuietly() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    when(jdbcTemplate.update(anyString())).thenReturn(0);
    CurationExpirationStatusJob job = new CurationExpirationStatusJob(jdbcTemplate);

    assertThatCode(() -> job.executeInternal(null)).doesNotThrowAnyException();
  }

  private ArgumentMatcher<String> nativeExpirationUpdateSql() {
    return sql ->
        sql.contains("UPDATE curation")
            && sql.contains("is_active = false")
            && sql.contains("is_active = true")
            && sql.contains("exposure_end_date < CURDATE()");
  }
}
