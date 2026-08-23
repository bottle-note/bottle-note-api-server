package app.bottlenote.alcohols.repository;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.redis.config.LettuceClientSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Tag("unit")
@DisplayName("RedisAlcoholLookupSnapshotStore 읽기/쓰기 타임아웃 분리 테스트")
class RedisAlcoholLookupSnapshotStoreTest {
  private LettuceConnectionFactory sourceFactory;
  private RedisAlcoholLookupSnapshotStore store;

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.destroy();
    }
    if (sourceFactory != null) {
      sourceFactory.destroy();
    }
  }

  @Test
  @DisplayName("lookup 읽기는 2초 전용 factory를 쓰고 쓰기는 공용 factory를 유지한다")
  void constructor_splitsReadTimeoutFromWriteTemplate() throws Exception {
    sourceFactory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("127.0.0.1", 6379),
            LettuceClientSupport.clientConfiguration(Duration.ofSeconds(15)));
    LettuceClientSupport.applySharedConnectionPolicy(sourceFactory);
    LettuceClientSupport.start(sourceFactory);
    RedisTemplate<String, Object> writeTemplate = new RedisTemplate<>();
    writeTemplate.setConnectionFactory(sourceFactory);
    writeTemplate.setKeySerializer(new StringRedisSerializer());
    writeTemplate.setValueSerializer(new StringRedisSerializer());
    writeTemplate.afterPropertiesSet();

    store =
        new RedisAlcoholLookupSnapshotStore(
            writeTemplate, sourceFactory, new ObjectMapper(), Duration.ofSeconds(2));

    RedisTemplate<?, ?> readTemplate = field(store, "readTemplate", RedisTemplate.class);
    RedisTemplate<?, ?> writeField = field(store, "writeTemplate", RedisTemplate.class);
    LettuceConnectionFactory readFactory =
        (LettuceConnectionFactory) readTemplate.getConnectionFactory();
    ClientOptions options = readFactory.getClientConfiguration().getClientOptions().orElseThrow();
    SocketOptions socketOptions = options.getSocketOptions();

    assertThat(writeField.getConnectionFactory()).isSameAs(sourceFactory);
    assertThat(readFactory).isNotSameAs(sourceFactory);
    assertThat(readFactory.getClientConfiguration().getCommandTimeout())
        .isEqualTo(Duration.ofSeconds(2));
    assertThat(sourceFactory.getClientConfiguration().getCommandTimeout())
        .isEqualTo(Duration.ofSeconds(15));
    assertThat(socketOptions.getKeepAlive().isEnabled()).isTrue();
    assertThat(socketOptions.getTcpUserTimeout().isEnabled()).isTrue();
    assertThat(socketOptions.getTcpUserTimeout().getTcpUserTimeout())
        .isEqualTo(Duration.ofSeconds(30));
    assertThat(options.getDisconnectedBehavior())
        .isEqualTo(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
    assertThat(readFactory.getShareNativeConnection()).isTrue();
    assertThat(readFactory.getValidateConnection()).isFalse();
  }

  @SuppressWarnings("unchecked")
  private static <T> T field(Object target, String name, Class<T> type) throws Exception {
    Field field = RedisAlcoholLookupSnapshotStore.class.getDeclaredField(name);
    field.setAccessible(true);
    return (T) field.get(target);
  }
}
