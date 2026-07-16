package app.batch.bottlenote.visitor;

import static app.batch.bottlenote.visitor.VisitorTelemetryMessageTest.validFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("batch")
@Testcontainers
@DisplayName("[batch] VisitorTelemetry Redis Stream 소비")
class VisitorTelemetryStreamConsumerIntegrationTest {

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
          .withDatabaseName("bottlenote_batch")
          .withUsername("root")
          .withPassword("root");

  @Container
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7.0.12"));

  private static JdbcTemplate jdbcTemplate;
  private static StringRedisTemplate redisTemplate;
  private VisitorTelemetryProperties properties;
  private VisitorTelemetryStreamConsumer consumer;

  @BeforeAll
  static void initializeInfrastructure() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute(
        """
        CREATE TABLE api_request_events (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          stream_event_id VARCHAR(41) NOT NULL UNIQUE,
          occurred_at DATETIME(6) NOT NULL,
          visitor_id CHAR(64) NOT NULL,
          trace_id VARCHAR(64) NULL,
          http_method VARCHAR(10) NOT NULL,
          request_path VARCHAR(2048) NOT NULL,
          request_uri VARCHAR(1024) NOT NULL,
          normalized_request_path VARCHAR(512) NOT NULL,
          status_code SMALLINT UNSIGNED NOT NULL,
          duration_ms BIGINT UNSIGNED NOT NULL,
          device_type VARCHAR(20) NOT NULL,
          operating_system VARCHAR(30) NOT NULL,
          browser VARCHAR(30) NOT NULL,
          browser_major_version VARCHAR(20) NULL,
          is_webview BIT(1) NOT NULL,
          create_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        )
        """);

    RedisStandaloneConfiguration redisConfiguration =
        new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort());
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfiguration);
    connectionFactory.afterPropertiesSet();
    connectionFactory.start();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
  }

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    jdbcTemplate.update("DELETE FROM api_request_events");
    properties = new VisitorTelemetryProperties();
    properties.setConsumerName("integration-consumer");
    properties.setBlockTimeout(Duration.ofMillis(50));
    properties.setRetryBackoff(Duration.ofMillis(50));
    properties.setPendingIdle(Duration.ofMillis(20));
    consumer = newConsumer();
  }

  @AfterEach
  void tearDown() {
    consumer.stop();
  }

  @Test
  @DisplayName("기존 Stream부터 소비해 DB 저장 후 ACK하고 삭제한다")
  void 기존_Stream을_소비하고_저장한다() {
    redisTemplate.opsForStream().add(properties.getStreamKey(), validFields());

    consumer.start();

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(rowCount()).isEqualTo(1);
      assertThat(redisTemplate.opsForStream().size(properties.getStreamKey())).isZero();
      assertThat(
              redisTemplate
                  .opsForStream()
                  .pending(properties.getStreamKey(), properties.getGroupName())
                  .getTotalPendingMessages())
          .isZero();
    });
  }

  @Test
  @DisplayName("같은 Stream ID를 다시 저장해도 DB 행은 중복되지 않는다")
  void Stream_ID로_멱등_저장한다() {
    VisitorTelemetryJdbcWriter writer = writer();
    VisitorTelemetryMessage message = VisitorTelemetryMessage.from("1000-0", validFields());

    writer.write(java.util.List.of(message));
    writer.write(java.util.List.of(message));

    assertThat(rowCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("DB 실패 메시지는 Pending에 남고 복구 후 다시 저장한다")
  void DB_실패_후_Pending을_재처리한다() {
    redisTemplate.opsForStream().add(properties.getStreamKey(), validFields());
    jdbcTemplate.execute("RENAME TABLE api_request_events TO api_request_events_unavailable");
    consumer.start();

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
        assertThat(
                redisTemplate
                    .opsForStream()
                    .pending(properties.getStreamKey(), properties.getGroupName())
                    .getTotalPendingMessages())
            .isEqualTo(1));

    jdbcTemplate.execute("RENAME TABLE api_request_events_unavailable TO api_request_events");

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(rowCount()).isEqualTo(1);
      assertThat(redisTemplate.opsForStream().size(properties.getStreamKey())).isZero();
    });
  }

  @Test
  @DisplayName("영구 오류 메시지는 Dead Letter로 격리한다")
  void 잘못된_메시지를_격리한다() {
    Map<String, String> invalid = validFields();
    invalid.remove("visitor_id");
    redisTemplate.opsForStream().add(properties.getStreamKey(), invalid);

    consumer.start();

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(rowCount()).isZero();
      assertThat(redisTemplate.opsForStream().size(properties.getStreamKey())).isZero();
      assertThat(redisTemplate.opsForStream().size(properties.getDeadLetterStreamKey()))
          .isEqualTo(1);
      StreamOperations<String, String, String> streamOperations = redisTemplate.opsForStream();
      MapRecord<String, String, String> deadLetter =
          streamOperations.range(properties.getDeadLetterStreamKey(), Range.unbounded()).getFirst();
      assertThat(deadLetter.getValue()).containsKeys("original_stream_id", "error_reason");
    });
  }

  @Test
  @DisplayName("90일 초과 데이터만 10000건 단위로 삭제한다")
  void 보존기간이_지난_데이터를_삭제한다() {
    VisitorTelemetryMessage oldMessage =
        new VisitorTelemetryMessage(
            "old-0",
            java.time.LocalDateTime.now().minusDays(91),
            "a".repeat(64),
            null,
            "GET",
            "/api/v1/old",
            "/api/v1/old",
            "/api/v1/old",
            200,
            1,
            "데스크톱",
            "macOS",
            "Chrome",
            "143",
            false);
    VisitorTelemetryMessage recentMessage =
        new VisitorTelemetryMessage(
            "recent-0",
            java.time.LocalDateTime.now().minusDays(89),
            "b".repeat(64),
            null,
            "GET",
            "/api/v1/recent",
            "/api/v1/recent",
            "/api/v1/recent",
            200,
            1,
            "데스크톱",
            "macOS",
            "Chrome",
            "143",
            false);
    writer().write(java.util.List.of(oldMessage, recentMessage));

    new VisitorTelemetryRetentionJob(jdbcTemplate).executeInternal(null);

    assertThat(rowCount()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT stream_event_id FROM api_request_events", String.class))
        .isEqualTo("recent-0");
  }

  @Test
  @DisplayName("다른 Consumer가 남긴 오래된 Pending을 회수한다")
  void 오래된_Pending을_회수한다() throws InterruptedException {
    redisTemplate.opsForStream().add(properties.getStreamKey(), validFields());
    redisTemplate
        .opsForStream()
        .createGroup(properties.getStreamKey(), ReadOffset.from("0-0"), properties.getGroupName());
    redisTemplate
        .opsForStream()
        .read(
            Consumer.from(properties.getGroupName(), "stopped-consumer"),
            StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed()));
    Thread.sleep(30);

    consumer.start();

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(rowCount()).isEqualTo(1);
      assertThat(redisTemplate.opsForStream().size(properties.getStreamKey())).isZero();
    });
  }

  private VisitorTelemetryStreamConsumer newConsumer() {
    return new VisitorTelemetryStreamConsumer(redisTemplate, writer(), properties);
  }

  private VisitorTelemetryJdbcWriter writer() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    return new VisitorTelemetryJdbcWriter(
        jdbcTemplate,
        new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
  }

  private int rowCount() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM api_request_events", Integer.class);
  }
}
