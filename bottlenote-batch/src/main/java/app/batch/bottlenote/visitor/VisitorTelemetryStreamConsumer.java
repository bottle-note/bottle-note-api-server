package app.batch.bottlenote.visitor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
final class VisitorTelemetryStreamConsumer implements SmartLifecycle {

  private final StreamOperations<String, String, String> streamOperations;
  private final VisitorTelemetryJdbcWriter writer;
  private final VisitorTelemetryProperties properties;
  private volatile boolean running;
  private Thread worker;

  VisitorTelemetryStreamConsumer(
      StringRedisTemplate redisTemplate,
      VisitorTelemetryJdbcWriter writer,
      VisitorTelemetryProperties properties) {
    this.streamOperations = redisTemplate.opsForStream();
    this.writer = writer;
    this.properties = properties;
  }

  @Override
  public synchronized void start() {
    if (running) {
      return;
    }
    running = true;
    worker = new Thread(this::consume, "visitor-telemetry-consumer");
    worker.setDaemon(true);
    worker.start();
  }

  @Override
  public synchronized void stop() {
    running = false;
    if (worker != null) {
      worker.interrupt();
    }
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 100;
  }

  private void consume() {
    boolean groupReady = false;
    log.info(
        "VisitorTelemetry Consumer 시작 stream={} group={} consumer={}",
        properties.getStreamKey(),
        properties.getGroupName(),
        properties.getConsumerName());

    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        if (!groupReady) {
          ensureConsumerGroup();
          groupReady = true;
        }

        List<MapRecord<String, String, String>> claimed = claimAbandonedMessages();
        if (!claimed.isEmpty()) {
          process(claimed);
          continue;
        }

        List<MapRecord<String, String, String>> records = readNewMessages();
        if (records != null && !records.isEmpty()) {
          process(records);
        }
      } catch (RuntimeException exception) {
        groupReady = false;
        log.warn("VisitorTelemetry 소비 실패, 재시도합니다", exception);
        backoff(properties.getRetryBackoff());
      }
    }
    running = false;
    log.info("VisitorTelemetry Consumer 종료");
  }

  private void ensureConsumerGroup() {
    try {
      streamOperations.createGroup(
          properties.getStreamKey(), ReadOffset.from("0-0"), properties.getGroupName());
      log.info(
          "VisitorTelemetry Consumer Group 생성 stream={} group={}",
          properties.getStreamKey(),
          properties.getGroupName());
    } catch (RedisSystemException exception) {
      if (!containsBusyGroup(exception)) {
        throw exception;
      }
    }
  }

  private boolean containsBusyGroup(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current.getMessage() != null && current.getMessage().contains("BUSYGROUP")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private List<MapRecord<String, String, String>> readNewMessages() {
    return streamOperations.read(
        Consumer.from(properties.getGroupName(), properties.getConsumerName()),
        StreamReadOptions.empty()
            .count(properties.getBatchSize())
            .block(properties.getBlockTimeout()),
        StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed()));
  }

  private List<MapRecord<String, String, String>> claimAbandonedMessages() {
    PendingMessages pending =
        streamOperations.pending(
            properties.getStreamKey(),
            properties.getGroupName(),
            Range.unbounded(),
            properties.getBatchSize());
    if (pending.isEmpty()) {
      return List.of();
    }

    List<RecordId> ids = new ArrayList<>();
    for (PendingMessage message : pending) {
      if (message.getElapsedTimeSinceLastDelivery().compareTo(properties.getPendingIdle()) >= 0) {
        ids.add(message.getId());
      }
    }
    if (ids.isEmpty()) {
      return List.of();
    }

    return streamOperations.claim(
        properties.getStreamKey(),
        properties.getGroupName(),
        properties.getConsumerName(),
        XClaimOptions.minIdle(properties.getPendingIdle()).ids(ids));
  }

  private void process(List<MapRecord<String, String, String>> records) {
    List<VisitorTelemetryMessage> validMessages = new ArrayList<>();
    List<MapRecord<String, String, String>> validRecords = new ArrayList<>();

    for (MapRecord<String, String, String> record : records) {
      try {
        validMessages.add(VisitorTelemetryMessage.from(record.getId().getValue(), record.getValue()));
        validRecords.add(record);
      } catch (IllegalArgumentException exception) {
        deadLetter(record, exception.getMessage());
      }
    }

    writer.write(validMessages);
    acknowledgeAndDelete(validRecords);
    if (!validRecords.isEmpty()) {
      log.info("VisitorTelemetry DB 저장 완료 count={}", validRecords.size());
    }
  }

  private void deadLetter(MapRecord<String, String, String> record, String reason) {
    Map<String, String> fields = new LinkedHashMap<>(record.getValue());
    fields.put("original_stream_id", record.getId().getValue());
    fields.put("error_reason", reason == null ? "unknown validation error" : reason);

    MapRecord<String, String, String> deadLetter =
        StreamRecords.string(fields).withStreamKey(properties.getDeadLetterStreamKey());
    streamOperations.add(
        deadLetter,
        XAddOptions.maxlen(properties.getDeadLetterMaxLength()).approximateTrimming(true));
    acknowledgeAndDelete(List.of(record));
    log.warn("VisitorTelemetry 메시지 격리 streamId={} reason={}", record.getId(), reason);
  }

  private void acknowledgeAndDelete(List<MapRecord<String, String, String>> records) {
    if (records.isEmpty()) {
      return;
    }
    RecordId[] ids = records.stream().map(record -> record.getId()).toArray(RecordId[]::new);
    streamOperations.acknowledge(
        properties.getStreamKey(), properties.getGroupName(), ids);
    streamOperations.delete(properties.getStreamKey(), ids);
  }

  private void backoff(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
