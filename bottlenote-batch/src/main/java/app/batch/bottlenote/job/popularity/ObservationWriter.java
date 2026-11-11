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

  /**
   * 첫 컬럼이 alcohol_id인 관측·스냅샷 행을 적재한다.
   *
   * <p>없는 주류를 가리키는 행이 하나라도 있으면 FK 위반으로 관측 단계 전체가 실패하므로, 그런 행은 빼고 저장한다.
   */
  public void batchInsert(String sql, List<Object[]> rows) {
    if (rows.isEmpty()) {
      return;
    }
    Set<Long> alcoholIds =
        new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM alcohols", Long.class));
    List<Object[]> insertable = rows.stream().filter(row -> alcoholIds.contains(row[0])).toList();
    if (insertable.size() < rows.size()) {
      log.warn("존재하지 않는 주류의 행을 저장하지 않습니다. 제외={}행", rows.size() - insertable.size());
    }
    for (int from = 0; from < insertable.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, insertable.size());
      jdbcTemplate.batchUpdate(sql, insertable.subList(from, to));
    }
  }

  public JdbcTemplate jdbc() {
    return jdbcTemplate;
  }
}
