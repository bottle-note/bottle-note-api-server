package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// 이슈 #344 장소검색 계약. 기존 데이터 회귀와 피드 노출 여부를 함께 고정한다.
@Tag("unit")
@DisplayName("큐레이션 장소 필드 계약 단위 테스트")
class CurationPlaceFieldContractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final String TASTING = "whisky_tasting_event.json";
  private static final String PROGRAM = "program.json";

  private final CurationPayloadValidator validator = new CurationPayloadValidator(OBJECT_MAPPER);
  private final CurationFeedProjector projector = new CurationFeedProjector(OBJECT_MAPPER);

  @Test
  @DisplayName("장소검색이 필요한 필드는 address-search 렌더링 계약을 쓴다")
  void requestSpec_placeSearchFields_useAddressSearchStyle() throws IOException {
    JsonNode tasting = OBJECT_MAPPER.valueToTree(schema(TASTING, "Request")).path("properties");
    assertThat(tasting.path("placeName").path("x-field-style").asText())
        .isEqualTo("address-search");
    assertThat(tasting.path("zipCode").path("x-field-style").asText()).isEqualTo("address-search");

    JsonNode program = OBJECT_MAPPER.valueToTree(schema(PROGRAM, "Request")).path("properties");
    assertThat(program.path("placeName").path("x-field-style").asText())
        .isEqualTo("address-search");
    assertThat(program.path("address").path("x-field-style").asText()).isEqualTo("address-search");
    assertThat(program.path("zipCode").path("x-field-style").asText()).isEqualTo("address-search");
  }

  @Test
  @DisplayName("placeName과 zipCode는 optional이라 기존 required 계약을 늘리지 않는다")
  void requestSpec_newFields_areOptional() throws IOException {
    JsonNode required = OBJECT_MAPPER.valueToTree(schema(TASTING, "Request")).path("required");

    assertThat(required).hasSize(8);
    assertThat(required.toString()).doesNotContain("placeName").doesNotContain("zipCode");
  }

  @Test
  @DisplayName("placeName이 없는 기존 시음회 payload도 그대로 검증을 통과한다")
  void validate_whenLegacyPayloadHasNoPlaceName_passes() throws IOException {
    var errors =
        validator.validate(new MapBackedSchema(schema(TASTING, "Request")), legacyPayload());

    assertThat(errors).isEmpty();
  }

  @Test
  @DisplayName("zipCode가 5자가 아니면 검증이 거부한다")
  void validate_whenZipCodeLengthIsNotFive_rejects() throws IOException {
    Map<String, Object> payload = legacyPayload();
    payload.put("zipCode", "123");

    var errors = validator.validate(new MapBackedSchema(schema(TASTING, "Request")), payload);

    assertThat(errors).isNotEmpty();
  }

  @Test
  @DisplayName("placeName이 responseSpec 검증 대상이 되어도 기존 저장값이 상세 경로에서 통과한다")
  void validateResponse_whenLegacyPlaceNameExists_passes() throws IOException {
    // responseSpec에 placeName이 새로 들어가면서 상세 경로(materialize)의 검증 대상이 됐다.
    Map<String, Object> payload = legacyPayload();
    payload.put("placeName", "도시술");

    var errors = validator.validate(new MapBackedSchema(schema(TASTING, "Response")), payload);

    assertThat(errors).isEmpty();
  }

  @Test
  @DisplayName("placeName이 100자를 넘으면 상세 경로 검증이 거부한다")
  void validateResponse_whenPlaceNameTooLong_rejects() throws IOException {
    Map<String, Object> payload = legacyPayload();
    payload.put("placeName", "가".repeat(101));

    var errors = validator.validate(new MapBackedSchema(schema(TASTING, "Response")), payload);

    assertThat(errors).isNotEmpty();
  }

  @Test
  @DisplayName("placeName의 피드 정렬 위치는 시간(20)과 주소(30) 사이다")
  void responseSpec_placeNameFeedOrder_sitsBetweenTimeAndAddress() throws IOException {
    JsonNode properties = OBJECT_MAPPER.valueToTree(schema(TASTING, "Response")).path("properties");
    JsonNode placeName = properties.path("placeName").path("x-feed");

    assertThat(placeName.path("enabled").asBoolean()).isTrue();
    assertThat(placeName.path("role").asText()).isEqualTo("location");
    assertThat(placeName.path("order").asInt())
        .isGreaterThan(properties.path("eventTime").path("x-feed").path("order").asInt())
        .isLessThan(properties.path("barAddress").path("x-feed").path("order").asInt());
  }

  @Test
  @DisplayName("피드에는 placeName이 실리고 zipCode는 실리지 않는다")
  void extractFeedPayload_exposesPlaceNameButNotZipCode() throws IOException {
    Map<String, Object> payload = legacyPayload();
    payload.put("placeName", "보틀노트 테이스팅룸");
    payload.put("zipCode", "06236");

    JsonNode feedPayload =
        OBJECT_MAPPER.valueToTree(
            projector.extractFeedPayload(schema(TASTING, "Response"), payload));

    assertThat(feedPayload.path("placeName").asText()).isEqualTo("보틀노트 테이스팅룸");
    assertThat(feedPayload.has("zipCode")).isFalse();
  }

  @Test
  @DisplayName("네 스펙 문서 모두 스펙 자체 검증을 통과한다")
  void validateSpec_allSchemas_areValid() throws IOException {
    for (String resource : List.of(TASTING, PROGRAM)) {
      for (String suffix : List.of("Request", "Response")) {
        assertThat(
                validator.validateSpec(
                    resource + "#" + suffix, new MapBackedSchema(schema(resource, suffix))))
            .isEmpty();
      }
    }
  }

  private static Map<String, Object> legacyPayload() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("eventDate", "2026-06-21");
    payload.put("eventTime", "19:00");
    payload.put("barAddress", "서울 강남구 테헤란로 123");
    payload.put("detailAddress", "2층");
    payload.put("entryFee", 50000);
    payload.put("capacity", 12);
    payload.put("guideText", "시음회 안내");
    payload.put(
        "alcohols",
        List.of(
            Map.of(
                "source",
                "MANUAL",
                "alcohol",
                Map.of("korName", "테스트 위스키", "selectedTags", List.of("셰리")))));
    return payload;
  }

  private static Map<String, Object> schema(String resourceName, String suffix) throws IOException {
    JsonNode root =
        OBJECT_MAPPER.readTree(
            new ClassPathResource("openapi/curation/" + resourceName).getInputStream());
    return OBJECT_MAPPER.convertValue(
        root.path("components").path("schemas").properties().stream()
            .filter(entry -> entry.getKey().endsWith(suffix))
            .findFirst()
            .orElseThrow()
            .getValue(),
        MAP_TYPE);
  }
}
