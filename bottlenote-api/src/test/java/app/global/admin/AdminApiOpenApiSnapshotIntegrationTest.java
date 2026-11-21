package app.global.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * main에서 두 앱이 내던 OpenAPI 문서와 통합 앱의 문서를 비교한다.
 *
 * <p>스냅샷은 main(aafb311e)의 product-api와 admin-api를 실행해 받은 문서다. servers만 MockMvc 요청
 * 기준값(http://localhost)으로 바꿔 두었다. admin 문서는 paths 키(/v1/..., /v2/...)와
 * servers({origin}/admin/api)까지 admin 앱과 같아야 한다.
 */
@Tag("integration")
@DisplayName("[integration] admin API 흡수 OpenAPI 문서 스냅샷")
class AdminApiOpenApiSnapshotIntegrationTest extends AdminApiIntegrationTestSupport {

  private static final int MAX_REPORTED = 30;

  @Test
  @DisplayName("product 문서를 받을 때 main product 문서와 내용과 경로 순서가 같고 admin 엔드포인트가 섞이지 않는다")
  void product_문서는_main과_같다() throws Exception {
    JsonNode actual = fetch("/api/v1/openapi.product.json");
    JsonNode expected = snapshot("baseline/product-openapi-main.json");

    assertThat(differences(expected, actual)).isEmpty();
    assertThat(pathKeys(actual)).isEqualTo(pathKeys(expected));
  }

  @Test
  @DisplayName("admin 문서를 받을 때 main admin 문서와 paths 키, servers, operationId까지 같다")
  void admin_문서는_main과_같다() throws Exception {
    JsonNode actual = fetch(AdminApi.DOCS_PATH);
    JsonNode expected = snapshot("baseline/admin-openapi-main.json");

    assertThat(actual.at("/servers/0/url").asText()).isEqualTo("http://localhost/admin/api");
    assertThat(pathKeys(actual)).isEqualTo(pathKeys(expected));
    assertThat(differences(expected, actual)).isEmpty();
  }

  private JsonNode fetch(String path) throws Exception {
    MvcResult result =
        mockMvc
            .perform(MockMvcRequestBuilders.get(path).accept(MediaType.APPLICATION_JSON))
            .andReturn();
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  private JsonNode snapshot(String resource) throws IOException {
    try (InputStream in = new ClassPathResource(resource).getInputStream()) {
      return mapper.readTree(in);
    }
  }

  // 실패 메시지에 문서 전체가 아니라 달라진 위치만 남긴다
  private static List<String> differences(JsonNode expected, JsonNode actual) {
    List<String> found = new ArrayList<>();
    collectDifferences("", expected, actual, found);
    return found.size() > MAX_REPORTED ? found.subList(0, MAX_REPORTED) : found;
  }

  private static void collectDifferences(
      String pointer, JsonNode expected, JsonNode actual, List<String> found) {
    if (expected.getNodeType() != actual.getNodeType()) {
      found.add(pointer + " 타입 " + expected.getNodeType() + " != " + actual.getNodeType());
      return;
    }
    if (expected.isObject()) {
      Set<String> names = new TreeSet<>();
      expected.fieldNames().forEachRemaining(names::add);
      actual.fieldNames().forEachRemaining(names::add);
      for (String name : names) {
        String child = pointer + "/" + name;
        if (!expected.has(name) || !actual.has(name)) {
          found.add(child + (expected.has(name) ? " 누락" : " 추가"));
        } else {
          collectDifferences(child, expected.get(name), actual.get(name), found);
        }
      }
      return;
    }
    if (expected.isArray()) {
      if (expected.size() != actual.size()) {
        found.add(pointer + " 길이 " + expected.size() + " != " + actual.size());
        return;
      }
      for (int i = 0; i < expected.size(); i++) {
        collectDifferences(pointer + "/" + i, expected.get(i), actual.get(i), found);
      }
      return;
    }
    if (!expected.equals(actual)) {
      found.add(pointer + " " + expected + " != " + actual);
    }
  }

  private static List<String> pathKeys(JsonNode spec) {
    List<String> keys = new ArrayList<>();
    spec.path("paths").fieldNames().forEachRemaining(keys::add);
    return keys;
  }
}
