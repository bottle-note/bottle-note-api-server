package app.global.config;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import app.bottlenote.global.data.response.GlobalResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Optional;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * 성공 응답을 {@code GlobalResponse} 공통 형식으로 감싼다.
 *
 * <p>대부분의 컨트롤러는 {@code ResponseEntity<GlobalResponse>}를 반환해 스펙 생성기가 {@code data}에 담길 타입을 알아낼 수 없다.
 * 그래서 각 엔드포인트는 그 타입만 표준 {@code @ApiResponse}로 알려주고, 이 클래스가 그 타입을 {@code data} 자리로 옮긴 뒤 공통 필드를 덧붙인다.
 * 덕분에 공통 형식을 엔드포인트마다 반복해서 적지 않아도 된다.
 *
 * <p>공통 형식을 쓰지 않고 DTO를 그대로 내보내는 엔드포인트도 있다. 이들은 컨트롤러가 반환 타입에 본문 타입을 구체적으로 선언하고, 이 클래스는 그 선언을 보고 감싸기를
 * 건너뛴다. 예외 목록을 따로 두지 않으므로 코드와 문서가 어긋날 수 없다.
 */
@Component
public class GlobalResponseSchemaCustomizer implements OperationCustomizer {

  private static final String SUCCESS_STATUS_PREFIX = "2";
  private static final String OBJECT_TYPE = "object";

  private static final String SUCCESS = "success";
  private static final String CODE = "code";
  private static final String DATA = "data";
  private static final String ERRORS = "errors";
  private static final String META = "meta";

  /**
   * 일부 컨트롤러는 {@code ResponseEntity<GlobalResponse>}를 반환한다. 이때 추론되는 스키마는 공통 형식 그 자체이므로 {@code data}에
   * 담길 타입이 아니다. 그대로 두면 공통 형식 안에 공통 형식이 중첩된다.
   */
  private static final String ENVELOPE_SCHEMA_REF =
      Components.COMPONENTS_SCHEMAS_REF + GlobalResponse.class.getSimpleName();

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {
    if (operation.getResponses() == null || returnsBareDto(handlerMethod)) {
      return operation;
    }
    operation.getResponses().forEach(this::wrapIfSuccess);
    return operation;
  }

  /**
   * 공통 형식을 거치지 않고 DTO를 그대로 내보내는 엔드포인트인지 판단한다.
   *
   * <p>판단 근거는 컨트롤러가 선언한 반환 타입이다. {@code ResponseEntity<NonceResponse>}처럼 본문 타입을 구체적으로 적어두면 그 타입이 곧
   * 응답 본문이라는 뜻이므로 감싸지 않는다. {@code ResponseEntity<GlobalResponse>}는 공통 형식을 쓴다는 뜻이다. 덕분에 예외 목록을 따로
   * 관리하지 않고 코드 자체가 문서의 근거가 된다.
   */
  private boolean returnsBareDto(HandlerMethod handlerMethod) {
    ResolvableType returnType = ResolvableType.forMethodReturnType(handlerMethod.getMethod());
    if (!ResponseEntity.class.isAssignableFrom(returnType.toClass())) {
      return false;
    }
    Class<?> bodyType = returnType.getGeneric(0).resolve();
    return bodyType != null
        && bodyType != Object.class
        && !GlobalResponse.class.isAssignableFrom(bodyType);
  }

  private void wrapIfSuccess(String statusCode, ApiResponse response) {
    if (!statusCode.startsWith(SUCCESS_STATUS_PREFIX)) {
      return;
    }
    Optional<Schema<?>> declared = declaredSchema(response);
    if (declared.filter(this::alreadyWrapped).isPresent()) {
      return;
    }
    Schema<?> envelope = commonEnvelope(declared.orElseGet(ObjectSchema::new));
    response.setContent(
        new Content().addMediaType(APPLICATION_JSON_VALUE, new MediaType().schema(envelope)));
  }

  /** 엔드포인트가 알려준 data 타입. */
  private Optional<Schema<?>> declaredSchema(ApiResponse response) {
    Content content = response.getContent();
    if (content == null) {
      return Optional.empty();
    }
    // MediaType#getSchema가 raw 타입이라 witness가 필요하다
    return content.values().stream()
        .<Schema<?>>map(MediaType::getSchema)
        .filter(this::isMeaningful)
        .findFirst();
  }

  /** 타입 정보가 없는 빈 object와 공통 형식 자체는 힌트로 취급하지 않는다. */
  private boolean isMeaningful(Schema<?> schema) {
    return schema != null && !isEnvelopeItself(schema) && !isEmptyObject(schema);
  }

  private boolean isEmptyObject(Schema<?> schema) {
    return OBJECT_TYPE.equals(schema.getType())
        && schema.get$ref() == null
        && (schema.getProperties() == null || schema.getProperties().isEmpty());
  }

  private boolean isEnvelopeItself(Schema<?> schema) {
    return ENVELOPE_SCHEMA_REF.equals(schema.get$ref());
  }

  /** 이미 공통 형식으로 적혀 있는지. {@link #isEnvelopeItself}와 달리 참조가 아닌 펼쳐진 스키마를 본다. */
  private boolean alreadyWrapped(Schema<?> schema) {
    return schema.getProperties() != null && schema.getProperties().containsKey(DATA);
  }

  private Schema<?> commonEnvelope(Schema<?> data) {
    return new ObjectSchema()
        .addProperty(SUCCESS, new BooleanSchema().description("요청 처리 성공 여부"))
        .addProperty(CODE, new IntegerSchema().description("HTTP 상태 코드"))
        .addProperty(DATA, data.description("요청 결과 값"))
        .addProperty(
            ERRORS,
            new ArraySchema().items(new StringSchema()).description("오류 메시지 목록. 성공 시 비어 있다"))
        .addProperty(META, new ObjectSchema().description("서버 버전, 응답 시각, 페이징 정보 등 부가 정보"));
  }
}
