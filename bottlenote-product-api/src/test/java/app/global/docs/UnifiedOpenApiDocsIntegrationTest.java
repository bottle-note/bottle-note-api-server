package app.global.docs;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.annotation.SecurityPolicy.AuthType;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.fixture.AdminUserTestFactory;
import app.global.admin.AdminApi;
import app.global.admin.AdminApiIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * product 문서와 admin 문서를 합친 문서를 검증한다.
 *
 * <p>같은 컨텍스트에서 받은 product·admin 문서를 기준으로, 합친 문서가 두 문서의 operation을 빠짐없이 실제 전체 경로로 담고 이름 충돌을 덮어쓰지
 * 않았는지 본다. 기존 두 문서가 main과 같은지는 AdminApiOpenApiSnapshotIntegrationTest가 확인한다.
 */
@Tag("integration")
@DisplayName("[integration] product·admin 합친 OpenAPI 문서")
class UnifiedOpenApiDocsIntegrationTest extends AdminApiIntegrationTestSupport {

  private static final String PRODUCT_DOCS = "/api/v1/openapi.product.json";
  private static final Set<String> HTTP_METHODS =
      Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

  @Autowired private SecurityPolicyRegistry securityPolicyRegistry;
  @Autowired private AdminUserTestFactory adminUserTestFactory;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  @DisplayName("토큰 없이 합친 문서를 받을 때 200과 JSON 문서를 주고 servers는 product 문서처럼 하나다")
  void 토큰_없이_합친_문서를_받을_수_있다() throws Exception {
    MvcTestResult result = mockMvcTester.get().uri(UnifiedOpenApiDocs.PATH).exchange();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    JsonNode unified = readJson(result);
    assertThat(unified.path("openapi").asText())
        .isEqualTo(fetch(PRODUCT_DOCS).path("openapi").asText());
    assertThat(unified.path("servers")).hasSize(1);
    assertThat(unified.at("/servers/0/url").asText()).isEqualTo("http://localhost");
  }

  @Test
  @DisplayName("합친 문서의 operation 수와 경로 수를 셀 때 product 문서와 admin 문서의 합과 같다")
  void operation_수는_두_문서의_합과_같다() throws Exception {
    JsonNode product = fetch(PRODUCT_DOCS);
    JsonNode admin = fetch(AdminApi.DOCS_PATH);
    JsonNode unified = fetch(UnifiedOpenApiDocs.PATH);

    assertThat(operations(unified)).hasSize(operations(product).size() + operations(admin).size());
    assertThat(unified.path("paths").size())
        .isEqualTo(product.path("paths").size() + admin.path("paths").size());
  }

  @Test
  @DisplayName(
      "합친 문서의 경로를 볼 때 product operation은 /api, admin operation은 /admin/api 아래 실제 전체 경로로 들어간다")
  void operation은_실제_전체_경로로_들어간다() throws Exception {
    JsonNode product = fetch(PRODUCT_DOCS);
    JsonNode admin = fetch(AdminApi.DOCS_PATH);
    Map<String, JsonNode> unified = operations(fetch(UnifiedOpenApiDocs.PATH));

    operations(product).keySet().forEach(key -> assertThat(unified).as(key).containsKey(key));
    operations(admin)
        .keySet()
        .forEach(key -> assertThat(unified).as(key).containsKey(mounted(key)));
    unified
        .keySet()
        .forEach(
            key ->
                assertThat(path(key))
                    .as(key)
                    .matches(
                        candidate ->
                            candidate.startsWith("/api/")
                                || candidate.startsWith(AdminApi.PATH_PREFIX + "/v1/")
                                || candidate.startsWith(AdminApi.PATH_PREFIX + "/v2/")));
  }

  @Test
  @DisplayName("이름을 바꾼 항목이 있어도 각 operation의 내용은 참조를 풀었을 때 원래 문서와 같다")
  void operation_내용은_원래_문서와_같다() throws Exception {
    JsonNode product = fetch(PRODUCT_DOCS);
    JsonNode admin = fetch(AdminApi.DOCS_PATH);
    JsonNode unified = fetch(UnifiedOpenApiDocs.PATH);
    Map<String, JsonNode> unifiedOperations = operations(unified);

    List<String> differences = new ArrayList<>();
    collectContentDifferences(product, unified, unifiedOperations, false, differences);
    collectContentDifferences(admin, unified, unifiedOperations, true, differences);

    assertThat(differences).isEmpty();
  }

  @Test
  @DisplayName("합친 문서의 모든 $ref를 따라갈 때 문서 안의 항목으로 해석된다")
  void 모든_ref가_문서_안에서_해석된다() throws Exception {
    JsonNode unified = fetch(UnifiedOpenApiDocs.PATH);
    List<String> refs = new ArrayList<>();
    collectRefs(unified, refs);

    assertThat(refs).isNotEmpty();
    assertThat(refs)
        .allSatisfy(
            ref -> {
              assertThat(ref).startsWith("#/");
              assertThat(unified.at(ref.substring(1)).isMissingNode()).as(ref).isFalse();
            });
  }

  @Test
  @DisplayName("합친 문서의 operationId를 모을 때 비어 있지 않고 모두 유일하다")
  void operationId는_유일하다() throws Exception {
    List<String> operationIds =
        operations(fetch(UnifiedOpenApiDocs.PATH)).values().stream()
            .map(operation -> operation.path("operationId").asText())
            .toList();

    assertThat(operationIds).doesNotContain("").doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("합친 문서의 태그를 볼 때 이름이 유일하고 operation이 쓰는 태그는 모두 선언되어 있다")
  void 태그는_유일하고_모두_선언되어_있다() throws Exception {
    JsonNode unified = fetch(UnifiedOpenApiDocs.PATH);
    List<String> declared = new ArrayList<>();
    unified.path("tags").forEach(tag -> declared.add(tag.path("name").asText()));

    assertThat(declared).doesNotHaveDuplicates();
    operations(unified)
        .values()
        .forEach(
            operation ->
                operation.path("tags").forEach(tag -> assertThat(declared).contains(tag.asText())));
  }

  @Test
  @DisplayName("합친 문서 경로의 보안 정책을 볼 때 product 문서처럼 JSON은 공개, YAML은 기본 정책이다")
  void 보안_정책은_product_문서와_같다() {
    assertThat(securityPolicyRegistry.resolve("GET", UnifiedOpenApiDocs.PATH))
        .isEqualTo(securityPolicyRegistry.resolve("GET", PRODUCT_DOCS))
        .isEqualTo(AuthType.PUBLIC);
    assertThat(securityPolicyRegistry.resolve("GET", UnifiedOpenApiDocs.YAML_PATH))
        .isEqualTo(securityPolicyRegistry.resolve("GET", PRODUCT_DOCS + ".yaml"))
        .isEqualTo(AuthType.REQUIRED_AUTH);
  }

  @Test
  @DisplayName("Origin을 붙여 합친 문서를 받을 때 product 문서와 같은 문서 전용 CORS를 쓴다")
  void CORS는_product_문서와_같다() {
    for (String origin : List.of("https://bottle-note.github.io", "https://bottle-note.com")) {
      MvcTestResult product = withOrigin(PRODUCT_DOCS, origin);
      MvcTestResult unified = withOrigin(UnifiedOpenApiDocs.PATH, origin);

      assertThat(unified.getResponse().getStatus())
          .as(origin)
          .isEqualTo(product.getResponse().getStatus());
      assertThat(unified.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
          .as(origin)
          .isEqualTo(product.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
    assertThat(
            withOrigin(UnifiedOpenApiDocs.PATH, "https://bottle-note.github.io")
                .getResponse()
                .getStatus())
        .isEqualTo(200);
    assertThat(
            withOrigin(UnifiedOpenApiDocs.PATH, "https://bottle-note.com")
                .getResponse()
                .getStatus())
        .isEqualTo(403);
  }

  @Test
  @DisplayName("YAML 문서를 받을 때 토큰이 없으면 401이고 product 토큰이 있으면 JSON 문서와 같은 내용이다")
  void YAML_문서는_JSON과_같은_내용이다() throws Exception {
    MvcTestResult anonymous = mockMvcTester.get().uri(UnifiedOpenApiDocs.YAML_PATH).exchange();
    MvcTestResult authorized =
        mockMvcTester
            .get()
            .uri(UnifiedOpenApiDocs.YAML_PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken())
            .exchange();

    assertThat(anonymous.getResponse().getStatus()).isEqualTo(401);
    assertThat(authorized.getResponse().getStatus()).isEqualTo(200);
    assertThat(authorized.getResponse().getContentType()).isEqualTo("application/vnd.oai.openapi");
    JsonNode yaml =
        new ObjectMapper(new YAMLFactory())
            .readTree(authorized.getResponse().getContentAsString(StandardCharsets.UTF_8));
    assertThat(yaml).isEqualTo(fetch(UnifiedOpenApiDocs.PATH));
  }

  @Test
  @DisplayName("합친 문서를 받은 뒤에도 기존 product·admin 문서(JSON, YAML)는 바이트 단위로 그대로다")
  void 기존_문서는_바뀌지_않는다() throws Exception {
    String productToken = getToken();
    String adminToken = adminToken();
    Map<String, byte[]> before = existingDocuments(productToken, adminToken);

    mockMvcTester.get().uri(UnifiedOpenApiDocs.PATH).exchange();
    mockMvcTester
        .get()
        .uri(UnifiedOpenApiDocs.YAML_PATH)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + productToken)
        .exchange();
    Map<String, byte[]> after = existingDocuments(productToken, adminToken);

    before.forEach((path, bytes) -> assertThat(after.get(path)).as(path).isEqualTo(bytes));
  }

  private Map<String, byte[]> existingDocuments(String productToken, String adminToken) {
    Map<String, byte[]> documents = new LinkedHashMap<>();
    documents.put(PRODUCT_DOCS, body(PRODUCT_DOCS, null));
    documents.put(PRODUCT_DOCS + ".yaml", body(PRODUCT_DOCS + ".yaml", productToken));
    documents.put(AdminApi.DOCS_PATH, body(AdminApi.DOCS_PATH, null));
    documents.put(AdminApi.DOCS_PATH + ".yaml", body(AdminApi.DOCS_PATH + ".yaml", adminToken));
    return documents;
  }

  private byte[] body(String path, String token) {
    var request = mockMvcTester.get().uri(path);
    if (token != null) {
      request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    MvcTestResult result = request.exchange();
    assertThat(result.getResponse().getStatus()).as(path).isEqualTo(200);
    return result.getResponse().getContentAsByteArray();
  }

  private String adminToken() {
    AdminUser admin = adminUserTestFactory.persistRootAdmin();
    return jwtTokenProvider
        .generateAdminToken(admin.getEmail(), admin.getRoles(), admin.getId())
        .accessToken();
  }

  private MvcTestResult withOrigin(String path, String origin) {
    return mockMvcTester.get().uri(path).header(HttpHeaders.ORIGIN, origin).exchange();
  }

  private JsonNode fetch(String path) throws Exception {
    MvcTestResult result = mockMvcTester.get().uri(path).exchange();
    assertThat(result.getResponse().getStatus()).as(path).isEqualTo(200);
    return readJson(result);
  }

  private JsonNode readJson(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  // "METHOD /path" 형식의 키로 operation을 모은다
  private static Map<String, JsonNode> operations(JsonNode document) {
    Map<String, JsonNode> operations = new LinkedHashMap<>();
    document
        .path("paths")
        .fields()
        .forEachRemaining(
            path ->
                path.getValue()
                    .fields()
                    .forEachRemaining(
                        method -> {
                          if (HTTP_METHODS.contains(method.getKey())) {
                            operations.put(
                                method.getKey().toUpperCase() + " " + path.getKey(),
                                method.getValue());
                          }
                        }));
    return operations;
  }

  private static String mounted(String operationKey) {
    int space = operationKey.indexOf(' ');
    return operationKey.substring(0, space + 1)
        + AdminApi.PATH_PREFIX
        + operationKey.substring(space + 1);
  }

  private static String path(String operationKey) {
    return operationKey.substring(operationKey.indexOf(' ') + 1);
  }

  // operationId와 태그 이름은 충돌 때문에 바뀔 수 있으므로 태그는 설명으로 비교하고, 나머지는 $ref를 모두 풀어서 비교한다
  private static void collectContentDifferences(
      JsonNode source,
      JsonNode unified,
      Map<String, JsonNode> unifiedOperations,
      boolean admin,
      List<String> differences) {
    Map<String, String> sourceTags = tagDescriptions(source);
    Map<String, String> unifiedTags = tagDescriptions(unified);
    operations(source)
        .forEach(
            (key, operation) -> {
              String unifiedKey = admin ? mounted(key) : key;
              JsonNode merged = unifiedOperations.get(unifiedKey);
              if (merged == null) {
                differences.add(unifiedKey + " 누락");
                return;
              }
              if (!comparable(source, operation).equals(comparable(unified, merged))) {
                differences.add(unifiedKey + " 내용 다름");
              }
              if (!describedTags(operation, sourceTags)
                  .equals(describedTags(merged, unifiedTags))) {
                differences.add(unifiedKey + " 태그 다름");
              }
            });
  }

  private static JsonNode comparable(JsonNode document, JsonNode operation) {
    ObjectNode copy = operation.deepCopy();
    copy.remove("operationId");
    copy.remove("tags");
    return inline(document, copy, new ArrayDeque<>());
  }

  private static JsonNode inline(JsonNode document, JsonNode node, Deque<String> visiting) {
    if (node.isObject()) {
      ObjectNode copy = JsonNodeFactory.instance.objectNode();
      node.fields()
          .forEachRemaining(
              field -> {
                if ("$ref".equals(field.getKey()) && field.getValue().isTextual()) {
                  String ref = field.getValue().asText();
                  if (visiting.contains(ref)) {
                    // 순환 참조는 이름이 아니라 순환 위치로 비교한다
                    copy.set("$ref", TextNode.valueOf("<cycle:" + depthOf(visiting, ref) + ">"));
                    return;
                  }
                  visiting.push(ref);
                  copy.set("$ref", inline(document, document.at(ref.substring(1)), visiting));
                  visiting.pop();
                } else {
                  copy.set(field.getKey(), inline(document, field.getValue(), visiting));
                }
              });
      return copy;
    }
    if (node.isArray()) {
      var copy = JsonNodeFactory.instance.arrayNode();
      node.forEach(element -> copy.add(inline(document, element, visiting)));
      return copy;
    }
    return node;
  }

  private static int depthOf(Deque<String> visiting, String ref) {
    int depth = 0;
    for (String visited : visiting) {
      if (visited.equals(ref)) {
        return depth;
      }
      depth++;
    }
    return -1;
  }

  private static Map<String, String> tagDescriptions(JsonNode document) {
    Map<String, String> descriptions = new HashMap<>();
    document
        .path("tags")
        .forEach(
            tag -> descriptions.put(tag.path("name").asText(), tag.path("description").asText()));
    return descriptions;
  }

  private static List<String> describedTags(JsonNode operation, Map<String, String> descriptions) {
    List<String> described = new ArrayList<>();
    operation
        .path("tags")
        .forEach(tag -> described.add(descriptions.getOrDefault(tag.asText(), "?" + tag.asText())));
    return described;
  }

  private static void collectRefs(JsonNode node, List<String> refs) {
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              field -> {
                if ("$ref".equals(field.getKey()) && field.getValue().isTextual()) {
                  refs.add(field.getValue().asText());
                } else {
                  collectRefs(field.getValue(), refs);
                }
              });
    } else if (node.isArray()) {
      node.forEach(element -> collectRefs(element, refs));
    }
  }
}
