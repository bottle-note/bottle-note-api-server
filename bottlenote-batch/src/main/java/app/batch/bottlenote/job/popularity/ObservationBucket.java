package app.batch.bottlenote.job.popularity;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.batch.core.JobParameters;

/**
 * 관측 기간 값 객체.
 *
 * <p>기간은 시작을 포함하고 종료를 제외하는 {@code [startAt, endAt)} 반개구간이다. 행마다 {@code now()}를 다시 부르면 정시 경계에서 같은 실행의
 * 결과가 두 버킷으로 갈릴 수 있으므로, Job 실행 시각으로 한 번만 만든다.
 */
public record ObservationBucket(BucketGranularity granularity, LocalDateTime startAt) {

  /** BatchQuartzJob이 넘기는 실행 시각 파라미터 이름. */
  public static final String EXECUTION_TIME_PARAM = "localDateTime";

  /** 인기도 Job이 별도로 선택한 닫힌 시간 버킷 파라미터 이름. */
  public static final String BUCKET_AT_PARAM = "observationBucketAt";

  public ObservationBucket {
    Objects.requireNonNull(granularity, "granularity는 null일 수 없습니다.");
    Objects.requireNonNull(startAt, "startAt은 null일 수 없습니다.");
    if (!granularity.startAt(startAt).equals(startAt)) {
      throw new IllegalArgumentException("startAt은 버킷 시작 시각이어야 합니다.");
    }
  }

  public static ObservationBucket of(BucketGranularity granularity, LocalDateTime dateTime) {
    Objects.requireNonNull(granularity, "granularity는 null일 수 없습니다.");
    return new ObservationBucket(granularity, granularity.startAt(dateTime));
  }

  public LocalDateTime endAt() {
    return granularity.endAt(startAt);
  }

  public boolean contains(LocalDateTime dateTime) {
    Objects.requireNonNull(dateTime, "dateTime은 null일 수 없습니다.");
    return !dateTime.isBefore(startAt) && dateTime.isBefore(endAt());
  }

  /** 기존 시간 관측 Job에서 사용하는 정시 절삭 호환 메서드. */
  public static LocalDateTime truncate(LocalDateTime executionTime) {
    return of(BucketGranularity.HOUR, executionTime).startAt();
  }

  /**
   * 잡 파라미터에서 버킷 시각을 얻는다.
   *
   * <p>기존 시간 관측 Job은 시작 시각만 저장하므로 호환을 위해 {@link LocalDateTime}을 반환한다. 신규 주간·월간 Job은
   * {@link #of(BucketGranularity, LocalDateTime)}으로 기간 전체를 사용한다.
   */
  public static LocalDateTime from(JobParameters jobParameters) {
    LocalDateTime selectedBucketAt =
        jobParameters == null ? null : jobParameters.getLocalDateTime(BUCKET_AT_PARAM);
    if (selectedBucketAt != null) {
      return truncate(selectedBucketAt);
    }
    LocalDateTime executionTime =
        jobParameters == null ? null : jobParameters.getLocalDateTime(EXECUTION_TIME_PARAM);
    return truncate(executionTime == null ? LocalDateTime.now() : executionTime);
  }
}
