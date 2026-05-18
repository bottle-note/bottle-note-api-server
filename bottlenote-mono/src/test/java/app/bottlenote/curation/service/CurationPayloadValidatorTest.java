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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("CurationPayloadValidator 단위 테스트")
class CurationPayloadValidatorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final CurationPayloadValidator validator = new CurationPayloadValidator(OBJECT_MAPPER);

  @Test
  @DisplayName("추천 위스키 requestSpec은 유효한 BOTTLE_NOTE 배열 payload를 허용한다")
  void validate_whenRecommendedWhiskyBottleNotePayloadIsValid_returnsEmptyErrors()
      throws IOException {
    Map<String, Object> requestSpec = schema("recommended_whisky.json", "Request");

    List<String> errors =
        validator.validate(
            new MapBackedSchema(requestSpec), List.of(recommendedItem("BOTTLE_NOTE")));

    assertThat(errors).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validRequestPayloadsBySpec")
  @DisplayName("큐레이션 스펙별 유효한 request payload일 경우 requestSpec 검증을 통과한다")
  void validate_whenPayloadMatchesEachRequestSpec_returnsEmptyErrors(
      String specCode, String resourceName, Object payload) throws IOException {
    Map<String, Object> requestSpec = schema(resourceName, "Request");

    List<String> errors = validator.validate(new MapBackedSchema(requestSpec), payload);

    assertThat(errors).as(specCode).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validResponsePayloadsBySpec")
  @DisplayName("큐레이션 스펙별 대표 materialized payload일 경우 responseSpec 검증을 통과한다")
  void validate_whenMaterializedPayloadMatchesEachResponseSpec_returnsEmptyErrors(
      String specCode, String resourceName, Object payload) throws IOException {
    Map<String, Object> responseSpec = schema(resourceName, "Response");

    List<String> errors = validator.validate(new MapBackedSchema(responseSpec), payload);

    assertThat(errors).as(specCode).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequestPayloadsBySpec")
  @DisplayName("큐레이션 스펙별 필수 payload가 누락될 경우 requestSpec 검증 오류를 반환한다")
  void validate_whenRequiredPayloadBySpecIsMissing_returnsErrors(
      String specCode, String resourceName, Object payload, String expectedError)
      throws IOException {
    Map<String, Object> requestSpec = schema(resourceName, "Request");

    List<String> errors = validator.validate(new MapBackedSchema(requestSpec), payload);

    assertThat(errors).as(specCode).contains(expectedError);
  }

  @Test
  @DisplayName("requestSpec의 x-container가 array이면 root payload는 비어 있거나 object일 수 없다")
  void validate_whenArrayContainerIsEmptyOrObject_returnsContainerErrors() throws IOException {
    Map<String, Object> requestSpec = schema("recommended_whisky.json", "Request");

    List<String> emptyErrors = validator.validate(new MapBackedSchema(requestSpec), List.of());
    List<String> objectErrors =
        validator.validate(new MapBackedSchema(requestSpec), recommendedItem("BOTTLE_NOTE"));

    assertThat(emptyErrors).containsExactly("payload 배열은 비어 있을 수 없습니다.");
    assertThat(objectErrors).containsExactly("$ payload는 array여야 합니다.");
  }

  @Test
  @DisplayName("requestSpec의 x-container가 object이면 root payload 배열을 허용하지 않는다")
  void validate_whenObjectContainerReceivesArray_returnsRootTypeError() throws IOException {
    Map<String, Object> requestSpec = schema("whisky_tasting_event.json", "Request");

    List<String> errors =
        validator.validate(
            new MapBackedSchema(requestSpec),
            List.of(tastingEventPayload(0, 1, List.of(recommendedItem("BOTTLE_NOTE")))));

    assertThat(errors).containsExactly("$ 타입이 object이어야 합니다.");
  }

  @Test
  @DisplayName("requestSpec은 required, enum, type 불일치를 상세 path로 검증한다")
  void validate_whenRequiredEnumOrTypeInvalid_returnsDetailedErrors() throws IOException {
    Map<String, Object> requestSpec = schema("recommended_whisky.json", "Request");
    Map<String, Object> invalid =
        map("source", "UNKNOWN", "alcohol", map("korName", 123, "selectedTags", List.of("셰리")));

    List<String> errors = validator.validate(new MapBackedSchema(requestSpec), List.of(invalid));

    assertThat(errors)
        .contains("$[0].source 값이 허용된 enum이 아닙니다.", "$[0].alcohol.korName 타입이 string이어야 합니다.");
  }

  @Test
  @DisplayName("requestSpec은 중첩 배열의 maxItems, minLength, maxLength 경계값을 검증한다")
  void validate_whenNestedArrayAndLengthBoundsInvalid_returnsErrors() throws IOException {
    Map<String, Object> requestSpec = schema("recommended_whisky.json", "Request");
    Map<String, Object> invalid =
        recommendedItem(
            "BOTTLE_NOTE",
            map(
                "alcoholId",
                1,
                "korName",
                "가".repeat(101),
                "selectedTags",
                List.of(
                    "셰리",
                    "",
                    "오크",
                    "피트",
                    "과일",
                    "바닐라",
                    "꿀",
                    "초콜릿",
                    "몰트",
                    "스모키",
                    "꽃",
                    "견과",
                    "긴태그".repeat(11))));

    List<String> errors = validator.validate(new MapBackedSchema(requestSpec), List.of(invalid));

    assertThat(errors)
        .contains(
            "$[0].alcohol.korName 문자열 길이는 최대 100자여야 합니다.",
            "$[0].alcohol.selectedTags 배열 크기는 최대 12개여야 합니다.",
            "$[0].alcohol.selectedTags[1] 문자열 길이는 최소 1자여야 합니다.",
            "$[0].alcohol.selectedTags[12] 문자열 길이는 최대 30자여야 합니다.");
  }

  @Test
  @DisplayName("시음회 requestSpec은 숫자와 배열의 최솟값, 최댓값 경계를 검증한다")
  void validate_whenTastingEventNumericAndArrayBoundsInvalid_returnsErrors() throws IOException {
    Map<String, Object> requestSpec = schema("whisky_tasting_event.json", "Request");
    Map<String, Object> invalid =
        tastingEventPayload(
            0,
            1000,
            List.of(
                recommendedItem("BOTTLE_NOTE"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL"),
                recommendedItem("MANUAL")));

    List<String> errors = validator.validate(new MapBackedSchema(requestSpec), invalid);

    assertThat(errors)
        .contains("$.capacity 값은 최대 999 이하여야 합니다.", "$.alcohols 배열 크기는 최대 10개여야 합니다.");
    assertThat(errors).doesNotContain("$.entryFee 값은 최소 0 이상이어야 합니다.");
  }

  @Test
  @DisplayName("시음회 requestSpec은 최소 경계값을 만족하면 통과한다")
  void validate_whenTastingEventMinimumBoundaryValid_returnsEmptyErrors() throws IOException {
    Map<String, Object> requestSpec = schema("whisky_tasting_event.json", "Request");

    List<String> errors =
        validator.validate(
            new MapBackedSchema(requestSpec),
            tastingEventPayload(0, 1, List.of(recommendedItem("BOTTLE_NOTE"))));

    assertThat(errors).isEmpty();
  }

  @Test
  @DisplayName("responseSpec은 materialized stats가 들어간 추천 위스키 응답 payload를 허용한다")
  void validate_whenRecommendedResponsePayloadMatchesResponseSpec_returnsEmptyErrors()
      throws IOException {
    Map<String, Object> responseSpec = schema("recommended_whisky.json", "Response");
    Map<String, Object> item =
        recommendedItem(
            "BOTTLE_NOTE",
            map("alcoholId", 1, "korName", "테스트 위스키", "selectedTags", List.of("셰리")));
    item.put(
        "stats",
        map("rating", 4.2, "totalRatingsCount", 10, "reviewCount", 3, "totalPickCount", 8));

    List<String> errors = validator.validate(new MapBackedSchema(responseSpec), List.of(item));

    assertThat(errors).isEmpty();
  }

  @Test
  @DisplayName("responseSpec은 stats 내부 타입까지 검증한다")
  void validate_whenResponseStatsTypeInvalid_returnsErrors() throws IOException {
    Map<String, Object> responseSpec = schema("recommended_whisky.json", "Response");
    Map<String, Object> item = recommendedItem("BOTTLE_NOTE");
    item.put(
        "stats",
        map("rating", "4.2", "totalRatingsCount", "10", "reviewCount", 3, "totalPickCount", 8));

    List<String> errors = validator.validate(new MapBackedSchema(responseSpec), List.of(item));

    assertThat(errors)
        .contains(
            "$[0].stats.rating 타입이 number이어야 합니다.",
            "$[0].stats.totalRatingsCount 타입이 integer이어야 합니다.");
  }

  private static Map<String, Object> schema(String resourceName, String suffix) throws IOException {
    JsonNode root =
        OBJECT_MAPPER.readTree(
            new ClassPathResource("openapi/curation/" + resourceName).getInputStream());
    JsonNode schemas = root.path("components").path("schemas");
    JsonNode schema =
        schemas.properties().stream()
            .filter(entry -> entry.getKey().endsWith(suffix))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow();
    return OBJECT_MAPPER.convertValue(schema, MAP_TYPE);
  }

  private static Map<String, Object> recommendedItem(String source) {
    return recommendedItem(
        source, map("alcoholId", 1, "korName", "테스트 위스키", "selectedTags", List.of("셰리")));
  }

  private static Map<String, Object> recommendedItem(String source, Map<String, Object> alcohol) {
    return map("source", source, "alcohol", alcohol, "comment", null);
  }

  private static Map<String, Object> pairingItem(String source) {
    Map<String, Object> item = recommendedItem(source);
    item.put(
        "pairings",
        List.of(
            map(
                "itemName",
                "다크 초콜릿",
                "itemImageUrl",
                "https://cdn.example.com/pairing/chocolate.jpg",
                "pairingNote",
                "셰리 향과 단맛을 이어준다.")));
    return item;
  }

  private static Map<String, Object> itemWithStats(Map<String, Object> item) {
    Map<String, Object> itemWithStats = new LinkedHashMap<>(item);
    itemWithStats.put(
        "stats",
        map("rating", 4.2, "totalRatingsCount", 10, "reviewCount", 3, "totalPickCount", 8));
    return itemWithStats;
  }

  private static Map<String, Object> withoutField(Map<String, Object> source, String field) {
    Map<String, Object> copy = new LinkedHashMap<>(source);
    copy.remove(field);
    return copy;
  }

  private static Map<String, Object> tastingEventPayload(
      int entryFee, int capacity, List<Map<String, Object>> alcohols) {
    return map(
        "eventDate",
        "2026-06-15",
        "eventTime",
        "19:30",
        "barAddress",
        "서울 강남구 테헤란로 123",
        "detailAddress",
        "2층 도시남 바",
        "isRecruiting",
        true,
        "entryFee",
        entryFee,
        "capacity",
        capacity,
        "applicationLink",
        "https://forms.example.com/tasting",
        "guideText",
        "시작 10분 전 입장해 주세요.",
        "alcohols",
        alcohols);
  }

  private static Stream<Arguments> validRequestPayloadsBySpec() {
    return Stream.of(
        Arguments.of(
            "RECOMMENDED_WHISKY",
            "recommended_whisky.json",
            List.of(recommendedItem("BOTTLE_NOTE"))),
        Arguments.of("WHISKY_PAIRING", "whisky_pairing.json", List.of(pairingItem("BOTTLE_NOTE"))),
        Arguments.of(
            "WHISKY_TASTING_EVENT",
            "whisky_tasting_event.json",
            tastingEventPayload(0, 1, List.of(recommendedItem("BOTTLE_NOTE")))));
  }

  private static Stream<Arguments> validResponsePayloadsBySpec() {
    return Stream.of(
        Arguments.of(
            "RECOMMENDED_WHISKY",
            "recommended_whisky.json",
            List.of(itemWithStats(recommendedItem("BOTTLE_NOTE")))),
        Arguments.of(
            "WHISKY_PAIRING",
            "whisky_pairing.json",
            List.of(itemWithStats(pairingItem("BOTTLE_NOTE")))),
        Arguments.of(
            "WHISKY_TASTING_EVENT",
            "whisky_tasting_event.json",
            tastingEventPayload(0, 1, List.of(itemWithStats(recommendedItem("BOTTLE_NOTE"))))));
  }

  private static Stream<Arguments> invalidRequestPayloadsBySpec() {
    return Stream.of(
        Arguments.of(
            "RECOMMENDED_WHISKY",
            "recommended_whisky.json",
            List.of(withoutField(recommendedItem("BOTTLE_NOTE"), "alcohol")),
            "$[0].alcohol 필드는 필수입니다."),
        Arguments.of(
            "WHISKY_PAIRING",
            "whisky_pairing.json",
            List.of(recommendedItem("BOTTLE_NOTE")),
            "$[0].pairings 필드는 필수입니다."),
        Arguments.of(
            "WHISKY_TASTING_EVENT",
            "whisky_tasting_event.json",
            withoutField(
                tastingEventPayload(0, 1, List.of(recommendedItem("BOTTLE_NOTE"))),
                "applicationLink"),
            "$.applicationLink 필드는 필수입니다."));
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
