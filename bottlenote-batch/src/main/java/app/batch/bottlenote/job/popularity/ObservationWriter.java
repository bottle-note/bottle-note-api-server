package app.batch.bottlenote.job.popularity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 관측 행 적재 공통 헬퍼.
 *
 * <p>같은 버킷을 다시 관측하는 경우(재실행)를 대비해 upsert로 쓴다. 유니크 키가 (alcohol_id, bucket_at)이므로 중복 실행이 행을 늘리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationWriter {

  private static final int BATCH_SIZE = 500;

  private final JdbcTemplate jdbcTemplate;

  /** 축 전체의 직전 관측 버킷. 희소 저장이라 주류별 직전과는 다를 수 있다. */
  public LocalDateTime findPreviousBucket(String table, LocalDateTime bucketAt) {
    return jdbcTemplate.queryForObject(
        "SELECT MAX(bucket_at) FROM " + table + " WHERE bucket_at < ?",
        LocalDateTime.class,
        bucketAt);
  }

  public void batchInsert(String sql, List<Object[]> rows) {
    if (rows.isEmpty()) {
      return;
    }
    for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, rows.size());
      jdbcTemplate.batchUpdate(sql, rows.subList(from, to));
    }
  }

  public JdbcTemplate jdbc() {
    return jdbcTemplate;
  }
}
