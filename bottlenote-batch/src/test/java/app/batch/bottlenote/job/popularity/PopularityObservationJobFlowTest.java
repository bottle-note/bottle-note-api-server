package app.batch.bottlenote.job.popularity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;

/**
 * Job 흐름 계약 검증.
 *
 * <p>네 축을 병렬로 돌린 뒤 합류해서 적재하는데, <b>한 축이라도 실패하면 적재가 실행되지 않아야 한다.</b> 이것이 이 파이프라인의 핵심 계약이라 실제로 Job을
 * 실행해서 확인한다.
 *
 * <p>인메모리 DB를 쓰므로 도커 없이 돈다. 관측 SQL 자체는 여기서 검증하지 않는다 — Tasklet을 기록용 테스트 더블로 갈아끼우고 흐름만 본다.
 */
@Tag("batch")
@DisplayName("[batch] 인기도 관측 Job 흐름")
@SpringBootTest(classes = PopularityObservationJobFlowTest.TestConfig.class)
@TestPropertySource(
    properties = {
      "spring.batch.job.enabled=false",
      "spring.batch.jdbc.initialize-schema=always"
    })
class PopularityObservationJobFlowTest {

  /** 어느 Step이 실제로 돌았는지 기록한다. 병렬이라 스레드 안전한 리스트를 쓴다. */
  static final List<String> EXECUTED = new CopyOnWriteArrayList<>();

  /** 이 축을 실패시킨다. null이면 전부 성공. */
  static volatile String failingAxis = null;

  @Autowired private JobLauncher jobLauncher;
  @Autowired private Job popularityObservationJob;

  private JobExecution run() throws Exception {
    EXECUTED.clear();
    JobParameters parameters =
        new JobParametersBuilder()
            .addLocalDateTime(ObservationBucket.EXECUTION_TIME_PARAM, LocalDateTime.now())
            .addString("run", String.valueOf(System.nanoTime()))
            .toJobParameters();
    return jobLauncher.run(popularityObservationJob, parameters);
  }

  @Test
  @DisplayName("네 축이 모두 성공하면 합류 후 최종 적재까지 실행된다")
  void allAxesSucceed_thenSnapshotRuns() throws Exception {
    failingAxis = null;

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(EXECUTED)
        .containsExactlyInAnyOrder("interest", "rating", "pick", "engagement", "snapshot");
  }

  @Test
  @DisplayName("한 축이 실패하면 최종 적재는 실행되지 않는다")
  void oneAxisFails_thenSnapshotDoesNotRun() throws Exception {
    failingAxis = "pick";

    JobExecution execution = run();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    // 성공한 축의 관측은 이미 일어났고, 적재만 건너뛴다
    assertThat(EXECUTED).doesNotContain("snapshot");
    assertThat(EXECUTED).contains("pick");
  }

  @Test
  @DisplayName("축이 실패해도 나머지 축의 관측은 그대로 수행된다")
  void oneAxisFails_othersStillObserve() throws Exception {
    failingAxis = "interest";

    run();

    // 병렬이라 실패 시점에 따라 일부가 덜 돌 수 있지만, 실패 축 자신은 반드시 시도된다
    assertThat(EXECUTED).contains("interest");
    assertThat(EXECUTED).doesNotContain("snapshot");
  }

  @Configuration
  @EnableAutoConfiguration
  @Import(PopularityObservationJobConfig.class)
  static class TestConfig {

    @Bean
    public DataSource dataSource() {
      return new EmbeddedDatabaseBuilder()
          .setType(EmbeddedDatabaseType.H2)
          .setName("popularity-flow-" + System.nanoTime())
          .build();
    }

    /** 실행 사실만 남기고, 지정된 축이면 실패한다. unchecked라 시그니처를 넓히지 않는다. */
    private static RepeatStatus record(String axis) {
      EXECUTED.add(axis);
      if (axis.equals(failingAxis)) {
        throw new IllegalStateException(axis + " 축 관측을 의도적으로 실패시킨다");
      }
      return RepeatStatus.FINISHED;
    }

    // JobConfig가 구체 타입을 주입받으므로 같은 타입으로 갈아끼운다
    @Bean
    public InterestObservationTasklet interestObservationTasklet() {
      return new InterestObservationTasklet(null) {
        @Override
        public RepeatStatus execute(StepContribution c, ChunkContext ctx) {
          return record("interest");
        }
      };
    }

    @Bean
    public RatingObservationTasklet ratingObservationTasklet() {
      return new RatingObservationTasklet(null) {
        @Override
        public RepeatStatus execute(StepContribution c, ChunkContext ctx) {
          return record("rating");
        }
      };
    }

    @Bean
    public PickObservationTasklet pickObservationTasklet() {
      return new PickObservationTasklet(null) {
        @Override
        public RepeatStatus execute(StepContribution c, ChunkContext ctx) {
          return record("pick");
        }
      };
    }

    @Bean
    public EngagementObservationTasklet engagementObservationTasklet() {
      return new EngagementObservationTasklet(null) {
        @Override
        public RepeatStatus execute(StepContribution c, ChunkContext ctx) {
          return record("engagement");
        }
      };
    }

    @Bean
    public PopularitySnapshotTasklet popularitySnapshotTasklet() {
      return new PopularitySnapshotTasklet(null, new PopularityObservationProperties()) {
        @Override
        public RepeatStatus execute(StepContribution c, ChunkContext ctx) {
          return record("snapshot");
        }
      };
    }
  }
}
