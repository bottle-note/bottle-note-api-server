package app.global.docs;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlMetrics;
import app.bottlenote.global.security.accesscontrol.AccessControlProperties;
import app.bottlenote.global.security.accesscontrol.AccessControlService;
import app.bottlenote.global.security.accesscontrol.AccessControlService.Decision;
import app.bottlenote.global.security.accesscontrol.AccessControlStore;
import app.bottlenote.global.security.accesscontrol.BanSnapshotHolder;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import app.bottlenote.global.security.constant.MaliciousPathPattern;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 합친 문서 경로가 운영 설정에서 product 문서 경로와 같은 대우를 받는지 확인한다.
 *
 * <p>테스트 classpath의 application.yml은 운영 파일을 가리므로 운영 파일을 직접 읽어 product access-control 설정을 만든다.
 */
@Tag("unit")
@DisplayName("[unit] 합친 OpenAPI 문서 경로의 운영 access-control·악성 경로 판정")
class UnifiedOpenApiDocsAccessControlTest {

  private static final String CLIENT_IP = "203.0.113.30";

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/v1/openapi.product.json",
        "/api/v1/openapi.product.json.yaml",
        "/api/v1/openapi.json",
        "/api/v1/openapi.json.yaml"
      })
  @DisplayName("문서 경로를 운영 product 설정으로 평가할 때 rate limit 없이 허용하고 버킷을 쓰지 않는다")
  void 문서_경로는_rate_limit에서_제외된다(String path) throws IOException {
    RecordingAccessControlStore store = new RecordingAccessControlStore();
    AccessControlProperties properties = bindProduction("bottlenote.access-control");
    AccessControlService service =
        new AccessControlService(
            store,
            properties,
            AccessControlMetrics.noop(),
            new BanSnapshotHolder(properties.getSnapshot().getMaxEntries()),
            Clock.systemUTC());

    Decision decision = service.evaluate(CLIENT_IP, path, "GET");

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.limit()).isEqualTo(-1);
    assertThat(store.consumedKeys).isEmpty();
    assertThat(properties.getExcludedPathPrefixes()).contains(UnifiedOpenApiDocs.PATH);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/openapi.json", "/api/v1/openapi.json.yaml"})
  @DisplayName("합친 문서 경로를 악성 경로 규칙으로 판정할 때 product 문서처럼 막지 않는다")
  void 합친_문서_경로는_악성_경로가_아니다(String path) {
    assertThat(MaliciousPathPattern.matches(new MockHttpServletRequest("GET", path))).isFalse();
    assertThat(
            MaliciousPathPattern.matches(
                new MockHttpServletRequest("GET", "/api/v1/openapi.product.json")))
        .isFalse();
  }

  // 첫 문서가 프로파일 조건 없는 기본 설정이다
  private static AccessControlProperties bindProduction(String prefix) throws IOException {
    List<PropertySource<?>> documents =
        new YamlPropertySourceLoader()
            .load("application", new FileSystemResource("src/main/resources/application.yml"));
    return new Binder(ConfigurationPropertySources.from(documents.get(0)))
        .bind(prefix, AccessControlProperties.class)
        .get();
  }

  private static final class RecordingAccessControlStore extends InMemoryAccessControlStore {

    private final List<String> consumedKeys = new ArrayList<>();

    @Override
    public AccessControlStore.ConsumeResult tryConsume(String key, int limit, Duration window) {
      consumedKeys.add(key);
      return super.tryConsume(key, limit, window);
    }
  }
}
