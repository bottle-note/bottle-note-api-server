package app.global.config;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 오류가 실리는 자리의 스키마를 한곳에 모은다.
 *
 * <p>같은 모양이 두 곳에 나온다. 성공 응답의 {@code errors}는 비어 있는 같은 배열이고, 실패 응답은 그 배열에 항목이 담긴 본문이다. 한곳에서 만들어야 둘이
 * 어긋나지 않는다.
 */
final class ErrorResponseSchema {

  /** 오류 항목 하나. */
  private static final String ITEM_NAME = "Error";

  /** 실패 응답 본문 전체. */
  private static final String ENVELOPE_NAME = "ErrorResponse";

  private static final String ITEM_REF = Components.COMPONENTS_SCHEMAS_REF + ITEM_NAME;
  private static final String ENVELOPE_REF = Components.COMPONENTS_SCHEMAS_REF + ENVELOPE_NAME;

  /**
   * meta 자리의 예시.
   *
   * <p>{@code MetaService.createMetaInfo()}를 그대로 쓰면 스펙을 만들 때마다 시각이 바뀌어 문서가 매번 달라진다. 값은 고정하되 키가 실제와
   * 어긋나지 않는지는 테스트가 지킨다.
   */
  static final Map<String, Object> EXAMPLE_META = exampleMeta();

  private static Map<String, Object> exampleMeta() {
    // Map.of는 순회 순서가 실행마다 달라져 스펙이 흔들린다
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("serverVersion", "1.0.0");
    meta.put("serverPathVersion", "v1");
    meta.put("serverEncoding", "UTF-8");
    meta.put("serverResponseTime", "2026-01-01T00:00:00");
    return Collections.unmodifiableMap(meta);
  }

  private ErrorResponseSchema() {}

  /** 스펙 전체에서 한 번만 정의하고 각 응답은 참조만 한다. */
  static void registerInto(Components components) {
    components.addSchemas(ITEM_NAME, errorItem());
    components.addSchemas(ENVELOPE_NAME, envelope());
  }

  /**
   * 실패 응답 하나. 본문 예시는 실제 예외 코드로 만든다.
   *
   * <p>스키마만 주면 문서 도구가 배열마다 항목을 하나씩 지어내 {@code data}가 비어 있지 않은 것처럼 보인다. 본문 전체를 예시로 박아 실제로 돌아오는 모양을
   * 그대로 보여준다.
   */
  static ApiResponse response(ExceptionCode sample, String description) {
    return new ApiResponse()
        .description(description)
        .content(
            new Content()
                .addMediaType(
                    APPLICATION_JSON_VALUE,
                    new MediaType()
                        .schema(new Schema<>().$ref(ENVELOPE_REF))
                        .example(exampleBody(sample))));
  }

  /** {@code GlobalResponse.error(Error)}가 만드는 본문과 같은 모양. */
  private static Map<String, Object> exampleBody(ExceptionCode sample) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("code", sample.getHttpStatus().value());
    body.put("data", List.of());
    body.put("errors", List.of(exampleItem(sample)));
    body.put("meta", EXAMPLE_META);
    return body;
  }

  private static Map<String, Object> exampleItem(ExceptionCode sample) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("code", ((Enum<?>) sample).name());
    item.put("status", sample.getHttpStatus().name());
    item.put("message", sample.getMessage());
    return item;
  }

  /** 오류가 담기는 배열. */
  static Schema<?> errors() {
    return new ArraySchema().items(new Schema<>().$ref(ITEM_REF));
  }

  /**
   * 성공 응답에 실리는 {@code errors}. 항상 비어 있다.
   *
   * <p>담길 항목의 모양을 {@code items}로 적지 않는다. 담기는 것이 없기 때문이다. 적어두면 문서 도구가 그 모양으로 항목을 하나 지어내 성공 예시에 오류가
   * 담긴 것처럼 보여준다. 오류 항목의 모양은 실패 응답이 설명한다.
   */
  static Schema<?> emptyErrors() {
    return new ArraySchema().maxItems(0);
  }

  private static Schema<?> errorItem() {
    return new ObjectSchema()
        .title("오류 항목")
        .addProperty(
            "code",
            new StringSchema()
                .description("오류 코드. 클라이언트가 화면 분기에 쓰는 값이다")
                .example("ALCOHOL_ID_REQUIRED"))
        .addProperty("status", new StringSchema().description("HTTP 상태 이름").example("BAD_REQUEST"))
        .addProperty(
            "message", new StringSchema().description("사람이 읽는 오류 설명").example("알코올 식별자는 필수입니다."));
  }

  private static Schema<?> envelope() {
    return new ObjectSchema()
        .title("오류 응답")
        .addProperty("success", new BooleanSchema().description("요청 처리 성공 여부").example(false))
        .addProperty("code", new IntegerSchema().description("HTTP 상태 코드").example(400))
        .addProperty(
            "data",
            new ArraySchema()
                .items(new ObjectSchema())
                .maxItems(0)
                .description("오류 응답에서는 항상 빈 배열이다"))
        .addProperty("errors", errors().description("발생한 오류 목록"))
        .addProperty("meta", new ObjectSchema().description("서버 버전, 응답 시각 등 부가 정보"));
  }
}
