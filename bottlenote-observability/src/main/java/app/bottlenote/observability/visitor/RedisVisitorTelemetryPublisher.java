package app.bottlenote.observability.visitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
final class RedisVisitorTelemetryPublisher implements VisitorTelemetryPublisher {

  private final StringRedisTemplate redisTemplate;
  private final String streamKey;
  private final XAddOptions xAddOptions;

  RedisVisitorTelemetryPublisher(
      StringRedisTemplate redisTemplate, String streamKey, long maxLength) {
    this.redisTemplate = redisTemplate;
    this.streamKey = streamKey;
    this.xAddOptions = XAddOptions.maxlen(maxLength).approximateTrimming(true);
  }

  @Override
  public void publish(VisitorTelemetry telemetry) {
    try {
      MapRecord<String, String, String> record =
          StreamRecords.string(telemetry.toStreamFields()).withStreamKey(streamKey);
      redisTemplate.opsForStream().add(record, xAddOptions);
    } catch (RuntimeException exception) {
      log.warn("VisitorTelemetry Redis Stream 발행 실패 stream={}", streamKey, exception);
    }
  }
}
