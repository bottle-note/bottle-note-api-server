package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 관측 행 적재 공통 헬퍼.
 *
 * <p>같은 버킷을 다시 관측하는 경우(재실행)를 대비해 upsert로 쓴다. 유니크 키가 (bucket_granularity, bucket_at,
 * alcohol_id)이므로 중복 실행이 행을 늘리지 않는다.
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

  /**
   * 이번 버킷에 이미 적재된 주류.
   *
   * <p>재실행에서 값이 직전과 같다고 건너뛰면, 1회차가 잘못 쓴 행이 그대로 남아 하향 정정이 되지 않는다.
   */
  public Set<Long> findAlcoholIdsAt(
      String table, BucketGranularity bucketGranularity, LocalDateTime bucketAt) {
    return new HashSet<>(
        jdbcTemplate.queryForList(
            "SELECT alcohol_id FROM "
                + table
                + " WHERE bucket_granularity = ? AND bucket_at = ?",
            Long.class,
            bucketGranularity.name(),
            bucketAt));
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
