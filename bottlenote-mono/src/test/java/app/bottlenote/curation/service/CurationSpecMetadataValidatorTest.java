package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationPayloadValidator 스펙 메타데이터 단위 테스트")
class CurationSpecMetadataValidatorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private final CurationPayloadValidator validator = new CurationPayloadValidator(OBJECT_MAPPER);

  @Test
  @DisplayName("x-depends-on 대상 key가 같은 properties scope에 없으면 스펙 검증에 실패한다")
  void validateSpec_존재하지_않는_dependency_key_실패() throws Exception {
    Map<String, Object> schema =
        OBJECT_MAPPER.readValue(
            """
            {
              "type": "object",
              "properties": {
                "applicationLink": {
                  "type": "string",
                  "x-depends-on": [
                    {
                      "key": "missingField",
                      "value": "true",
                      "description": "모집 중일 때만 신청 링크를 노출한다."
                    }
                  ]
                },
                "isRecruiting": {
                  "type": "boolean"
                }
              }
            }
            """,
            MAP_TYPE);

    assertThat(validator.validateSpec("test#Request", new MapBackedSchema(schema)))
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("의존 대상 key가 같은 scope에 없습니다")
                    .contains("test#Request.applicationLink")
                    .contains("missingField"));
  }

  @Test
  @DisplayName("x-depends-on value가 대상 필드 타입으로 정규화 불가능하면 스펙 검증에 실패한다")
  void validateSpec_dependency_value_타입_정규화_실패() throws Exception {
    Map<String, Object> schema =
        OBJECT_MAPPER.readValue(
            """
            {
              "type": "object",
              "properties": {
                "applicationLink": {
                  "type": "string",
                  "x-depends-on": [
                    {
                      "key": "isRecruiting",
                      "value": "yes",
                      "description": "모집 중일 때만 신청 링크를 노출한다."
                    }
                  ]
                },
                "isRecruiting": {
                  "type": "boolean"
                }
              }
            }
            """,
            MAP_TYPE);

    assertThat(validator.validateSpec("test#Request", new MapBackedSchema(schema)))
        .anySatisfy(
            error ->
                assertThat(error)
                    .contains("value를 의존 대상 필드 타입으로 정규화할 수 없습니다")
                    .contains("isRecruiting")
                    .contains("yes"));
  }
}
