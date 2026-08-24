package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 보관 기간이 지난 HOUR 관측 정리.
 *
 * <p>월간 Job의 마지막 Step이다. 롤업과 Snapshot Step이 성공해야 도달하므로, 이번 달 롤업이 실패한 상태에서 원본이 지워지는 일은 없다.
 *
 * <p>삭제 자격은 주류마다 따로 본다. 어떤 주류의 WEEK·MONTH 행이 다섯 영역에 모두 있어야 그 주류의 HOUR를 지운다. 버킷 시각만 전역으로 비교하면 다른 주류의
 * 롤업 때문에 롤업이 빠진 주류의 원본까지 지워진다.
 *
 * <p>기준 시각은 대상 월에서 끌어온다. 실행 시각의 {@code now()}에 기대면 재실행마다 경계가 밀려 같은 입력이 다른 결과를 낸다.
 *
 * <p>삭제 대상은 그 HOUR가 속한 WEEK와 MONTH가 모두 기준 시각 안에서 닫힌 경우로 한정한다. 반쪽 기간을 잘라내면 남은 원본이 어느 기간의 일부인지 알 수 없다.
 *
 * <p>각 영역의 마지막 HOUR는 다음 관측의 증감·누적 계산 기준으로 남긴다. 희소 관측의 기준 행과 같은 시각의 Snapshot도 함께 남겨, 새 기준 행이 생긴 뒤 다음
 * 정리에서 이전 기준 행을 다시 찾을 수 있게 한다.
 *
 * <p>chunk마다 REQUIRES_NEW로 커밋한다. Step 트랜잭션 안에서 모두 지우면 잠금이 Step이 끝날 때까지 유지되고, 중간에 실패하면 지금까지의 정리도 함께
 * 되돌아간다. chunk 단위로 끊으면 잠금이 짧고, 일부만 성공한 뒤 재시도해도 남은 키부터 이어서 지운다.
 */
@Slf4j
@Component
public class HourObservationCleanupTasklet implements Tasklet {

  /** 정리 대상이자 상위 롤업 존재를 확인할 다섯 데이터 영역. 마지막이 삭제 후보를 고르는 기준 테이블이다. */
  static final List<String> TABLES =
      List.of(
          "alcohol_interest_observations",
          "alcohol_rating_observations",
          "alcohol_pick_observations",
          "alcohol_engagement_observations",
          "alcohol_popularity_snapshots");

  private static final String SNAPSHOT_TABLE = "alcohol_popularity_snapshots";

  /**
   * 한 트랜잭션이 다루는 주류 수.
   *
   * <p>주류 수가 이 값 안이면 시간 버킷 하나가 트랜잭션 하나로 끝난다. 더 잘게 끊으면 한 달 정리에 트랜잭션이 수만 개로 늘어나고, 더 키우면 한 트랜잭션이
   * 다섯 테이블에서 잠그는 행이 그만큼 길게 유지된다.
   */
  static final int CHUNK_SIZE = 5_000;

  /** 다음으로 정리할 HOUR 버킷. 전체 목록을 메모리에 올리지 않고 커서로 한 칸씩 나아간다. */
  private static final String FIRST_HOUR_SQL =
      "SELECT MIN(bucket_at) FROM "
          + SNAPSHOT_TABLE
          + " WHERE bucket_granularity = 'HOUR' AND bucket_at < ?";

  private static final String NEXT_HOUR_SQL = FIRST_HOUR_SQL + " AND bucket_at > ?";

  private static final String CANDIDATE_SQL = candidateSql();

  private final ObservationWriter writer;
  private final PopularityObservationProperties properties;
  private final TransactionTemplate chunkTransaction;

  public HourObservationCleanupTasklet(
      ObservationWriter writer,
      PopularityObservationProperties properties,
      PlatformTransactionManager transactionManager) {
    this.writer = writer;
    this.properties = properties;
    this.chunkTransaction = new TransactionTemplate(transactionManager);
    this.chunkTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * 삭제 후보 조회.
   *
   * <p>성공한 HOUR 실행은 관측 이력이 있는 주류마다 Snapshot 행을 남긴다. 그 키를 기준으로 삼으면 축 하나가 실패해 Snapshot이 없는 잔여 HOUR는
   * 자연히 보존된다.
   *
   * <p>같은 주류의 WEEK와 MONTH 행을 다섯 영역에서 모두 확인한다. 조건 열 개는 모두 유니크 키를 그대로 타는 존재 확인이다.
   */
  private static String candidateSql() {
    StringBuilder sql =
        new StringBuilder(
            "SELECT s.alcohol_id FROM "
                + SNAPSHOT_TABLE
                + " s WHERE s.bucket_granularity = 'HOUR' AND s.bucket_at = ?"
                + " AND EXISTS (SELECT 1 FROM "
                + SNAPSHOT_TABLE
                + " n WHERE n.alcohol_id = s.alcohol_id"
                + " AND n.bucket_granularity = 'HOUR' AND n.bucket_at > s.bucket_at)");
    for (String table : TABLES) {
      sql.append(rollupExists(table, BucketGranularity.WEEK));
      sql.append(rollupExists(table, BucketGranularity.MONTH));
    }
    return sql.append(" AND s.alcohol_id > ? ORDER BY s.alcohol_id LIMIT ")
        .append(CHUNK_SIZE)
        .toString();
  }

  private static String rollupExists(String table, BucketGranularity granularity) {
    return " AND EXISTS (SELECT 1 FROM "
        + table
        + " r WHERE r.alcohol_id = s.alcohol_id AND r.bucket_granularity = '"
        + granularity.name()
        + "' AND r.bucket_at = ?)";
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    LocalDateTime cutoff = cutoffFrom(parameters);

    int deleted = 0;
    int cleanedHours = 0;
    LocalDateTime hourAt = nextHour(null, cutoff);
    while (hourAt != null) {
      LocalDateTime weekAt = BucketGranularity.WEEK.startAt(hourAt);
      LocalDateTime monthAt = BucketGranularity.MONTH.startAt(hourAt);
      // 기간이 기준 시각 안에서 닫히지 않았으면 그 HOUR는 건드리지 않는다
      boolean closed =
          !BucketGranularity.WEEK.endAt(weekAt).isAfter(cutoff)
              && !BucketGranularity.MONTH.endAt(monthAt).isAfter(cutoff);
      if (closed) {
        int removed = cleanHour(hourAt, weekAt, monthAt);
        if (removed > 0) {
          cleanedHours++;
          deleted += removed;
        }
      }
      hourAt = nextHour(hourAt, cutoff);
    }

    contribution.incrementWriteCount(deleted);
    log.info("HOUR 관측 정리 완료. cutoff={}, 정리한 버킷={}개, 삭제={}행", cutoff, cleanedHours, deleted);
    return RepeatStatus.FINISHED;
  }

  /**
   * 정리 기준 시각.
   *
   * <p>대상 월이 닫힌 시각에서 보관 일수를 뺀다. 같은 월을 다시 실행해도 같은 경계가 나온다.
   */
  private LocalDateTime cutoffFrom(JobParameters parameters) {
    String granularity =
        parameters == null ? null : parameters.getString(PopularityRollupTasklet.GRANULARITY_PARAM);
    LocalDateTime bucketAt =
        parameters == null ? null : parameters.getLocalDateTime(ObservationBucket.BUCKET_AT_PARAM);
    if (!BucketGranularity.MONTH.name().equals(granularity) || bucketAt == null) {
      throw new IllegalArgumentException("정리 기준을 정할 대상 월 파라미터가 없습니다.");
    }
    ObservationBucket target = new ObservationBucket(BucketGranularity.MONTH, bucketAt);
    return BucketGranularity.HOUR.startAt(
        target.endAt().minusDays(properties.getRetention().getHourDays()));
  }

  private LocalDateTime nextHour(LocalDateTime cursor, LocalDateTime cutoff) {
    return cursor == null
        ? writer.jdbc().queryForObject(FIRST_HOUR_SQL, LocalDateTime.class, cutoff)
        : writer.jdbc().queryForObject(NEXT_HOUR_SQL, LocalDateTime.class, cutoff, cursor);
  }

  /** 한 HOUR 버킷을 chunk 단위로 비운다. 후보가 chunk보다 적으면 그 버킷은 끝난 것이다. */
  private int cleanHour(LocalDateTime hourAt, LocalDateTime weekAt, LocalDateTime monthAt) {
    int deleted = 0;
    long cursor = Long.MIN_VALUE;
    Chunk chunk;
    do {
      chunk = deleteChunk(hourAt, weekAt, monthAt, cursor);
      deleted += chunk.deleted();
      cursor = chunk.lastAlcoholId();
    } while (chunk.candidates() == CHUNK_SIZE);
    return deleted;
  }

  /**
   * 후보 조회와 다섯 영역 삭제를 한 트랜잭션에서 끝낸다.
   *
   * <p>일부 영역만 지워진 상태로 커밋되면 그 주류의 기간이 반쪽으로 남는다. 조회를 바깥 트랜잭션에 두면 REPEATABLE READ에서 이미 커밋된 삭제가 보이지 않아
   * 같은 후보를 계속 다시 읽는다.
   */
  private Chunk deleteChunk(
      LocalDateTime hourAt, LocalDateTime weekAt, LocalDateTime monthAt, long cursor) {
    Chunk chunk =
        chunkTransaction.execute(
            status -> {
              List<Long> alcoholIds = candidates(hourAt, weekAt, monthAt, cursor);
              if (alcoholIds.isEmpty()) {
                return Chunk.EMPTY;
              }
              return new Chunk(
                  alcoholIds.size(), delete(hourAt, alcoholIds), alcoholIds.getLast());
            });
    return chunk == null ? Chunk.EMPTY : chunk;
  }

  private List<Long> candidates(
      LocalDateTime hourAt, LocalDateTime weekAt, LocalDateTime monthAt, long cursor) {
    List<Object> arguments = new ArrayList<>(2 + TABLES.size() * 2);
    arguments.add(hourAt);
    for (int index = 0; index < TABLES.size(); index++) {
      arguments.add(weekAt);
      arguments.add(monthAt);
    }
    arguments.add(cursor);
    return writer.jdbc().queryForList(CANDIDATE_SQL, Long.class, arguments.toArray());
  }

  private int delete(LocalDateTime hourAt, List<Long> alcoholIds) {
    int removed = 0;
    Set<Long> snapshotAnchors = new HashSet<>();
    for (String table : TABLES.subList(0, TABLES.size() - 1)) {
      HourRows hourRows = classifyHourRows(table, hourAt, alcoholIds);
      snapshotAnchors.addAll(hourRows.anchors());
      removed += deleteRows(table, hourAt, hourRows.deletable());
    }

    List<Long> deletableSnapshots = new ArrayList<>(alcoholIds);
    deletableSnapshots.removeAll(snapshotAnchors);
    removed += deleteRows(SNAPSHOT_TABLE, hourAt, deletableSnapshots);
    return removed;
  }

  /** 현재 행 뒤에 같은 주류의 HOUR가 있는지에 따라 삭제 행과 기준 행을 나눈다. */
  private HourRows classifyHourRows(
      String table, LocalDateTime hourAt, List<Long> alcoholIds) {
    String placeholders = placeholders(alcoholIds.size());
    List<Long> deletable = new ArrayList<>();
    List<Long> anchors = new ArrayList<>();
    writer
        .jdbc()
        .query(
            "SELECT o.alcohol_id, CASE WHEN EXISTS (SELECT 1 FROM "
                + table
                + " n WHERE n.alcohol_id = o.alcohol_id"
                + " AND n.bucket_granularity = 'HOUR' AND n.bucket_at > o.bucket_at)"
                + " THEN 1 ELSE 0 END AS has_newer FROM "
                + table
                + " o WHERE o.bucket_granularity = 'HOUR' AND o.bucket_at = ?"
                + " AND o.alcohol_id IN ("
                + placeholders
                + ")",
            rs -> {
              List<Long> target = rs.getBoolean("has_newer") ? deletable : anchors;
              target.add(rs.getLong("alcohol_id"));
            },
            arguments(hourAt, alcoholIds));
    return new HourRows(deletable, anchors);
  }

  private int deleteRows(String table, LocalDateTime hourAt, List<Long> alcoholIds) {
    if (alcoholIds.isEmpty()) {
      return 0;
    }
    return writer
        .jdbc()
        .update(
            "DELETE FROM "
                + table
                + " WHERE bucket_granularity = 'HOUR' AND bucket_at = ?"
                + " AND alcohol_id IN ("
                + placeholders(alcoholIds.size())
                + ")",
            arguments(hourAt, alcoholIds));
  }

  private String placeholders(int size) {
    return "?,".repeat(size - 1) + "?";
  }

  private Object[] arguments(LocalDateTime hourAt, List<Long> alcoholIds) {
    Object[] arguments = new Object[alcoholIds.size() + 1];
    arguments[0] = hourAt;
    for (int index = 0; index < alcoholIds.size(); index++) {
      arguments[index + 1] = alcoholIds.get(index);
    }
    return arguments;
  }

  /** 한 트랜잭션이 처리한 후보 수와 삭제 행 수. 마지막 주류 ID 다음부터 이어서 처리한다. */
  private record Chunk(int candidates, int deleted, long lastAlcoholId) {
    static final Chunk EMPTY = new Chunk(0, 0, Long.MIN_VALUE);
  }

  private record HourRows(List<Long> deletable, List<Long> anchors) {}
}
