package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 스펙 문서의 품질을 전수 검사한다.
 *
 * <p>문서 어노테이션을 붙이지 않은 컨트롤러는 {@link #PENDING_TAGS}에 등재해 검사를 건너뛴다. 도메인 문서화를 마칠 때마다 해당 항목을 지우면 그 시점부터
 * 이 테스트가 감시를 시작한다. 목록이 비면 전수 검사가 예외 없이 통과하는 상태가 된다.
 */
@Tag("integration")
@DisplayName("[integration] OpenAPI 스펙 품질")
class OpenApiSpecQualityTest extends OpenApiSpecTestSupport {

  /**
   * 아직 문서 어노테이션을 붙이지 않은 컨트롤러의 기본 태그 이름.
   *
   * <p>springdoc은 태그를 지정하지 않으면 컨트롤러 클래스명을 kebab-case로 바꿔 태그로 쓴다. 이 목록이 곧 남은 작업량이다.
   */
  private static final Set<String> PENDING_TAGS = Set.of();

  @Test
  @DisplayName("문서화한 엔드포인트의 태그는 클래스 이름이 아닌 도메인 이름이다")
  void 태그는_클래스_이름이_아니다() throws Exception {
    var violations =
        documentedOperations().stream()
            .filter(operation -> operation.tag().endsWith("-controller"))
            .map(SpecOperation::toString)
            .toList();

    assertThat(violations)
        .withFailMessage(
            "태그가 컨트롤러 클래스명으로 남아 있습니다. 도메인 이름을 지정하세요:%n%s", String.join("%n", violations))
        .isEmpty();
  }

  @Test
  @DisplayName("문서화한 엔드포인트는 모두 요약 문장을 갖는다")
  void 모든_엔드포인트가_요약을_갖는다() throws Exception {
    var violations =
        documentedOperations().stream()
            .filter(operation -> operation.summary().isBlank())
            .map(SpecOperation::toString)
            .toList();

    assertThat(violations)
        .withFailMessage("요약(summary)이 없습니다:%n%s", String.join("%n", violations))
        .isEmpty();
  }

  @Test
  @DisplayName("요약은 메서드 이름을 그대로 옮긴 것이 아니다")
  void 요약은_메서드_이름이_아니다() throws Exception {
    var violations =
        documentedOperations().stream()
            .filter(operation -> operation.summary().equals(operation.operationId()))
            .map(SpecOperation::toString)
            .toList();

    assertThat(violations)
        .withFailMessage("요약이 메서드 이름과 같습니다. 사람이 읽는 문장으로 쓰세요:%n%s", String.join("%n", violations))
        .isEmpty();
  }

  @Test
  @DisplayName("문서화한 엔드포인트의 응답 data는 실제 타입을 가리킨다")
  void 응답_data가_실제_타입을_가리킨다() throws Exception {
    var violations =
        documentedOperations().stream()
            .filter(SpecOperation::hasEmptyDataSchema)
            .map(SpecOperation::toString)
            .toList();

    assertThat(violations)
        .withFailMessage("응답 data가 빈 object입니다. 담기는 타입을 알려주세요:%n%s", String.join("%n", violations))
        .isEmpty();
  }

  @Test
  @DisplayName("미문서화 목록에 이미 문서화된 태그가 남아 있지 않다")
  void 미문서화_목록이_최신_상태다() throws Exception {
    var liveTags = operationsOf(fetchSpec()).stream().map(SpecOperation::tag).toList();
    var stale = PENDING_TAGS.stream().filter(tag -> !liveTags.contains(tag)).sorted().toList();

    assertThat(stale)
        .withFailMessage(
            "이미 문서화된 태그가 미문서화 목록에 남아 있습니다. PENDING_TAGS에서 지우세요:%n%s", String.join("%n", stale))
        .isEmpty();
  }

  /** 미문서화 목록에 없는, 즉 검사 대상인 엔드포인트. */
  private List<SpecOperation> documentedOperations() throws Exception {
    return operationsOf(fetchSpec()).stream()
        .filter(operation -> !PENDING_TAGS.contains(operation.tag()))
        .toList();
  }
}
