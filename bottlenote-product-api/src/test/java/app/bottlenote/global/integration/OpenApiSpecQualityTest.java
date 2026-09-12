package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** 스펙 문서의 품질을 전수 검사한다. 예외를 두지 않으므로 모든 엔드포인트가 검사 대상이다. */
@Tag("integration")
@DisplayName("[integration] OpenAPI 스펙 품질")
class OpenApiSpecQualityTest extends OpenApiSpecTestSupport {

  /** OpenAPI가 components 하위 키에 허용하는 형식. */
  private static final Pattern COMPONENT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

  @Test
  @DisplayName("엔드포인트의 태그는 클래스 이름이 아닌 도메인 이름이다")
  void 태그는_클래스_이름이_아니다() {
    assertNoOperation(
        "태그가 컨트롤러 클래스명으로 남아 있습니다. 도메인 이름을 지정하세요",
        operation -> operation.tag().endsWith("-controller"));
  }

  @Test
  @DisplayName("모든 엔드포인트는 요약 문장을 갖는다")
  void 모든_엔드포인트가_요약을_갖는다() {
    assertNoOperation("요약(summary)이 없습니다", operation -> operation.summary().isBlank());
  }

  @Test
  @DisplayName("요약은 메서드 이름을 그대로 옮긴 것이 아니다")
  void 요약은_메서드_이름이_아니다() {
    assertNoOperation(
        "요약이 메서드 이름과 같습니다. 사람이 읽는 문장으로 쓰세요",
        operation -> operation.summary().equals(operation.operationId()));
  }

  @Test
  @DisplayName("응답 data는 실제 타입을 가리킨다")
  void 응답_data가_실제_타입을_가리킨다() {
    assertNoOperation("응답 data가 빈 object입니다. 담기는 타입을 알려주세요", SpecOperation::hasEmptyDataSchema);
  }

  @Test
  @DisplayName("모든 파라미터는 schema, content 또는 참조를 갖는다")
  void 모든_파라미터가_스키마_근거를_갖는다() {
    var spec = fetchSpec();
    var pathViolations =
        spec.at("/paths").properties().stream()
            .flatMap(
                path ->
                    parameterViolations(
                        path.getKey() + " [path]", path.getValue().path("parameters")));
    var operationViolations =
        operationsOf(spec).stream()
            .flatMap(
                operation ->
                    parameterViolations(
                        operation.endpoint(), operation.definition().path("parameters")));
    var violations = Stream.concat(pathViolations, operationViolations).toList();

    assertThat(violations)
        .withFailMessage("파라미터에 schema, content 또는 $ref가 없습니다:%n%s", joined(violations))
        .isEmpty();
  }

  @Test
  @DisplayName("components 하위 이름은 OpenAPI가 허용하는 식별자 형식이다")
  void components_이름이_식별자_형식이다() {
    var components = fetchSpec().at("/components");
    var violations =
        components.properties().stream()
            .flatMap(
                group ->
                    childNamesOf(group.getValue())
                        .filter(name -> !COMPONENT_NAME_PATTERN.matcher(name).matches())
                        .map(name -> group.getKey() + "." + name))
            .toList();

    assertThat(violations)
        .withFailMessage(
            """
            components 하위 이름이 OpenAPI 규칙(%s)을 위반합니다. \
            @Schema(name=...)이나 보안 스키마 이름에는 영문 식별자를 쓰고 한국어는 title이나 \
            description으로 옮기세요. 위반하면 Scalar 같은 문서 도구가 스펙을 거부합니다:%n%s""",
            COMPONENT_NAME_PATTERN.pattern(), joined(violations))
        .isEmpty();
  }

  @Test
  @DisplayName("태그 목록에 같은 이름이 두 번 실리지 않는다")
  void 태그_이름은_유일하다() {
    var names = declaredTagNames(fetchSpec());
    var duplicated =
        names.stream().filter(name -> Collections.frequency(names, name) > 1).distinct().toList();

    assertThat(duplicated)
        .withFailMessage(
            """
            같은 태그 이름이 여러 번 실렸습니다. 태그는 OpenApiConfig에서만 선언하고 \
            문서 어노테이션에는 이름만 남기세요:%n%s""",
            joined(duplicated))
        .isEmpty();
  }

  @Test
  @DisplayName("엔드포인트가 사용하는 태그는 모두 태그 목록에 선언되어 있다")
  void 사용하는_태그가_모두_선언되어_있다() {
    var spec = fetchSpec();
    var declared = Set.copyOf(declaredTagNames(spec));
    var violations =
        operationsOf(spec).stream()
            .flatMap(
                operation ->
                    operation.tags().stream()
                        .filter(tag -> !declared.contains(tag))
                        .map(tag -> "%s - %s".formatted(operation.endpoint(), tag)))
            .distinct()
            .toList();

    assertThat(violations)
        .withFailMessage(
            """
            OpenApiConfig에 없는 태그를 사용하고 있습니다. 이름을 맞추거나 태그를 선언하세요. \
            선언되지 않은 태그는 설명 없는 메뉴로 문서에 끼어듭니다:%n%s""",
            joined(violations))
        .isEmpty();
  }

  private List<String> declaredTagNames(JsonNode spec) {
    return StreamSupport.stream(spec.at("/tags").spliterator(), false)
        .map(tag -> tag.path("name").asText())
        .toList();
  }

  private Stream<String> parameterViolations(String owner, JsonNode parameters) {
    return StreamSupport.stream(parameters.spliterator(), false)
        .filter(parameter -> !hasParameterSchema(parameter))
        .map(parameter -> "%s - %s".formatted(owner, parameter.path("name").asText("<unnamed>")));
  }

  private boolean hasParameterSchema(JsonNode parameter) {
    return !parameter.path("$ref").asText("").isBlank()
        || !parameter.path("schema").isEmpty()
        || !parameter.path("content").isEmpty();
  }

  /** 조건을 위반하는 엔드포인트가 하나도 없어야 한다. */
  private void assertNoOperation(String reason, Predicate<SpecOperation> violating) {
    var violations =
        operationsOf(fetchSpec()).stream().filter(violating).map(SpecOperation::toString).toList();

    assertThat(violations).withFailMessage("%s:%n%s", reason, joined(violations)).isEmpty();
  }
}
