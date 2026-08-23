package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.BucketGranularity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

@Tag("batch")
@DisplayName("[batch] 관측 버킷 시각")
class ObservationBucketTest {

  @Test
  @DisplayName("시간 버킷은 시작을 포함하고 종료를 제외한다")
  void hour_containsStartButExcludesEnd() {
    ObservationBucket bucket =
        ObservationBucket.of(BucketGranularity.HOUR, LocalDateTime.of(2026, 8, 23, 14, 37));

    assertThat(bucket.startAt()).isEqualTo(LocalDateTime.of(2026, 8, 23, 14, 0));
    assertThat(bucket.endAt()).isEqualTo(LocalDateTime.of(2026, 8, 23, 15, 0));
    assertThat(bucket.contains(bucket.startAt())).isTrue();
    assertThat(bucket.contains(bucket.endAt().minusNanos(1))).isTrue();
    assertThat(bucket.contains(bucket.endAt())).isFalse();
  }

  @Test
  @DisplayName("주 버킷은 월요일 자정부터 다음 월요일 자정 전까지다")
  void week_startsOnMondayAndEndsAtNextMonday() {
    ObservationBucket bucket =
        ObservationBucket.of(BucketGranularity.WEEK, LocalDateTime.of(2026, 8, 23, 23, 59));

    assertThat(bucket.startAt()).isEqualTo(LocalDateTime.of(2026, 8, 17, 0, 0));
    assertThat(bucket.endAt()).isEqualTo(LocalDateTime.of(2026, 8, 24, 0, 0));
    assertThat(bucket.contains(bucket.endAt())).isFalse();
  }

  @Test
  @DisplayName("월 버킷은 월초 자정부터 다음 달 월초 자정 전까지다")
  void month_usesCalendarMonthBoundaries() {
    ObservationBucket bucket =
        ObservationBucket.of(BucketGranularity.MONTH, LocalDateTime.of(2026, 2, 28, 23, 59));

    assertThat(bucket.startAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
    assertThat(bucket.endAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
    assertThat(bucket.contains(bucket.endAt())).isFalse();
  }

  @Test
  @DisplayName("버킷 시작 시각이 아닌 값으로는 값 객체를 만들 수 없다")
  void constructor_rejectsNonBoundaryStartAt() {
    assertThatThrownBy(
            () -> new ObservationBucket(BucketGranularity.HOUR, LocalDateTime.of(2026, 8, 23, 14, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("실행 시각이 정시가 아니어도 정시로 절삭한다")
  void truncate_dropsMinutesAndBelow() {
    LocalDateTime executionTime = LocalDateTime.of(2026, 8, 23, 14, 37, 51, 123_456_789);

    assertThat(ObservationBucket.truncate(executionTime))
        .isEqualTo(LocalDateTime.of(2026, 8, 23, 14, 0));
  }

  @Test
  @DisplayName("정각에 실행하면 그 시각이 그대로 버킷이 된다")
  void truncate_keepsExactHour() {
    LocalDateTime executionTime = LocalDateTime.of(2026, 8, 23, 14, 0);

    assertThat(ObservationBucket.truncate(executionTime)).isEqualTo(executionTime);
  }

  @Test
  @DisplayName("자정 직전에 실행해도 날짜가 넘어가지 않는다")
  void truncate_doesNotRollOverAtMidnightBoundary() {
    LocalDateTime executionTime = LocalDateTime.of(2026, 8, 23, 23, 59, 59);

    assertThat(ObservationBucket.truncate(executionTime))
        .isEqualTo(LocalDateTime.of(2026, 8, 23, 23, 0));
  }

  @Test
  @DisplayName("파라미터 이름이 배치가 실제로 넘기는 값과 같다")
  void parameterNameMatchesWhatTheSchedulerSends() {
    // BatchQuartzJob은 이 상수가 아니라 리터럴 "localDateTime"으로 파라미터를 넣는다.
    // 상수만 바꾸면 조회가 빗나가 각 축이 제각각 now()로 폴백하고, 정시 경계에서 버킷이 갈린다.
    // 다른 테스트는 모두 이 상수를 넣고 이 상수로 읽어 동어반복이므로 여기서만 잡을 수 있다.
    assertThat(ObservationBucket.EXECUTION_TIME_PARAM).isEqualTo("localDateTime");
  }

  @Test
  @DisplayName("잡 파라미터의 실행 시각을 버킷으로 쓴다")
  void from_usesJobParameter() {
    JobParameters parameters =
        new JobParametersBuilder()
            .addLocalDateTime(
                ObservationBucket.EXECUTION_TIME_PARAM, LocalDateTime.of(2026, 8, 23, 9, 42))
            .toJobParameters();

    assertThat(ObservationBucket.from(parameters))
        .isEqualTo(LocalDateTime.of(2026, 8, 23, 9, 0));
  }

  @Test
  @DisplayName("같은 파라미터로 여러 번 물어도 같은 버킷을 준다")
  void from_isStableAcrossCalls() {
    JobParameters parameters =
        new JobParametersBuilder()
            .addLocalDateTime(
                ObservationBucket.EXECUTION_TIME_PARAM, LocalDateTime.of(2026, 8, 23, 9, 42))
            .toJobParameters();

    // 축마다 따로 물어도 같은 값이어야 네 축이 한 버킷으로 묶인다
    assertThat(ObservationBucket.from(parameters)).isEqualTo(ObservationBucket.from(parameters));
  }

  @Test
  @DisplayName("실행 시각 파라미터가 없으면 현재 시각을 절삭해 쓴다")
  void from_fallsBackToNowWhenParameterMissing() {
    JobParameters empty = new JobParametersBuilder().toJobParameters();

    LocalDateTime bucket = ObservationBucket.from(empty);

    assertThat(bucket).isEqualTo(ObservationBucket.truncate(LocalDateTime.now()));
    assertThat(bucket.getMinute()).isZero();
    assertThat(bucket.getSecond()).isZero();
  }

  @Test
  @DisplayName("파라미터가 null이어도 예외 없이 현재 시각으로 대체한다")
  void from_handlesNullParameters() {
    assertThat(ObservationBucket.from(null)).isEqualTo(ObservationBucket.truncate(LocalDateTime.now()));
  }
}
