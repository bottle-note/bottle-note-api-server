package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("PROGRAM 일정 선택값 계약 단위 테스트")
class CurationProgramSpecContractTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final String RESOURCE = "openapi/curation/program.json";

  private final CurationPayloadValidator validator = new CurationPayloadValidator(OBJECT_MAPPER);
  private final CurationFeedProjector projector = new CurationFeedProjector(OBJECT_MAPPER);

  @Test
  @DisplayName("요청과 응답 일정은 이름, 유형, 설명만 필수로 선언한다")
  void programItems_requiredFields_areExactlyCoreFields() throws IOException {
    assertThat(requiredProgramFields("Request")).containsExactly("name", "type", "description");
    assertThat(requiredProgramFields("Response")).containsExactly("name", "type", "description");
  }

  @Test
  @DisplayName("요청과 응답의 행사 및 일정 신청 URL은 참가 신청 링크로 표시한다")
  void applicationUrls_displayName_isParticipationApplicationLink() throws IOException {
    for (String suffix : List.of("Request", "Response")) {
      JsonNode properties = OBJECT_MAPPER.valueToTree(schema(suffix)).path("properties");

      assertThat(properties.path("registrationUrl").path("x-display-name").asText())
          .isEqualTo("참가 신청 링크");
      assertThat(
              properties
                  .path("programs")
                  .path("items")
                  .path("properties")
                  .path("applicationUrl")
                  .path("x-display-name")
                  .asText())
          .isEqualTo("참가 신청 링크");
    }
  }

  @Test
  @DisplayName("날짜와 시간이 없는 최소 일정 payload는 요청과 응답 검증을 통과한다")
  void validate_minimalProgramWithoutSchedule_passesRequestAndResponse() throws IOException {
    Map<String, Object> payload = minimalPayload();

    assertThat(validator.validate(new MapBackedSchema(schema("Request")), payload)).isEmpty();
    assertThat(validator.validate(new MapBackedSchema(schema("Response")), payload)).isEmpty();
  }

  @Test
  @DisplayName("날짜와 시간이 없는 최소 일정 payload도 피드 필드를 생성한다")
  void extractFeedPayload_minimalProgramWithoutSchedule_keepsAllowedFeedFields()
      throws IOException {
    JsonNode feedPayload =
        OBJECT_MAPPER.valueToTree(
            projector.extractFeedPayload(schema("Response"), minimalPayload()));

    JsonNode program = feedPayload.path("programs").path(0);
    assertThat(program.path("name").asText()).isEqualTo("위스키 입문 클래스");
    assertThat(program.path("type").asText()).isEqualTo("MASTER_CLASS");
    assertThat(program.fieldNames()).toIterable().containsExactly("name", "type");
  }

  @Test
  @DisplayName("PROGRAM 리소스 버전은 3.0.2이다")
  void resourceVersion_isThreeDotZeroDotTwo() throws IOException {
    assertThat(resource().path("info").path("version").asText()).isEqualTo("3.0.2");
  }

  private static List<String> requiredProgramFields(String suffix) throws IOException {
    JsonNode required =
        OBJECT_MAPPER
            .valueToTree(schema(suffix))
            .path("properties")
            .path("programs")
            .path("items")
            .path("required");
    return StreamSupport.stream(required.spliterator(), false).map(JsonNode::asText).toList();
  }

  private static Map<String, Object> minimalPayload() {
    return Map.of(
        "eventStartDate", "2026-07-24",
        "eventEndDate", "2026-07-26",
        "placeName", "코엑스",
        "address", "서울 강남구 영동대로 513",
        "programs",
            List.of(
                Map.of(
                    "name", "위스키 입문 클래스",
                    "type", "MASTER_CLASS",
                    "description", "위스키를 처음 접하는 참가자를 위한 클래스입니다.")));
  }

  private static Map<String, Object> schema(String suffix) throws IOException {
    return OBJECT_MAPPER.convertValue(
        resource().path("components").path("schemas").path("Program" + suffix), MAP_TYPE);
  }

  private static JsonNode resource() throws IOException {
    return OBJECT_MAPPER.readTree(new ClassPathResource(RESOURCE).getInputStream());
  }
}
