package app.batch.bottlenote.job.popularity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.batch.core.JobParameters;

/**
 * 관측 버킷 시각.
 *
 * <p>네 축이 같은 버킷으로 묶이려면 잡 전체가 하나의 시각을 공유해야 한다. 행마다 now()를 다시 부르면 자정이나 정시 경계에서 같은 실행의 결과가 두 버킷으로
 * 갈린다.
 */
public final class ObservationBucket {

  /** BatchQuartzJob이 넘기는 실행 시각 파라미터 이름. */
  public static final String EXECUTION_TIME_PARAM = "localDateTime";

  private ObservationBucket() {}

  /** 정시로 절삭한다. 버킷 간격을 바꾸려면 여기만 고치면 된다. */
  public static LocalDateTime truncate(LocalDateTime executionTime) {
    return executionTime.truncatedTo(ChronoUnit.HOURS);
  }

  /**
   * 잡 파라미터에서 버킷 시각을 얻는다.
   *
   * <p>파라미터가 없으면(수동 실행 등) 현재 시각으로 대신한다.
   */
  public static LocalDateTime from(JobParameters jobParameters) {
    LocalDateTime executionTime =
        jobParameters == null ? null : jobParameters.getLocalDateTime(EXECUTION_TIME_PARAM);
    return truncate(executionTime == null ? LocalDateTime.now() : executionTime);
  }
}
