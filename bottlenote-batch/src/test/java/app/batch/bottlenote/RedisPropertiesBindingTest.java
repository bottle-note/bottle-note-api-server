package app.batch.bottlenote;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("Batch Redis 속성 바인딩 테스트")
class RedisPropertiesBindingTest {

  @Test
  @DisplayName("Sentinel 환경변수가 없으면 standalone 설정만 바인딩한다")
  void datasourceYaml_withoutSentinelEnvironment_bindsStandaloneOnly() throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    loader
        .load("batch-datasource", new ClassPathResource("application-datasource.yml"))
        .forEach(environment.getPropertySources()::addLast);

    RedisProperties properties =
        Binder.get(environment).bind("spring.data.redis", Bindable.of(RedisProperties.class)).get();

    assertThat(properties.getHost()).isEqualTo("localhost");
    assertThat(properties.getPort()).isEqualTo(16379);
    assertThat(properties.getSentinel()).isNull();
    assertThat(properties.getCluster()).isNull();
  }
}
