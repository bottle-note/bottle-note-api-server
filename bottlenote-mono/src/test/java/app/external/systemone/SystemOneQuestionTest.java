package app.external.systemone;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("SystemOne 요청 계약 단위 테스트")
class SystemOneQuestionTest {

  @Test
  @DisplayName("choice 선택지가 2~255개 범위를 벗어나면 생성할 때 예외가 발생한다")
  void choice_선택지_개수를_검증할_수_있다() {
    assertThatThrownBy(() -> new SystemOneQuestion.Choice("q", options(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SystemOneQuestion.Choice("q", options(256)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> new SystemOneQuestion.Choice("q", options(2))).doesNotThrowAnyException();
    assertThatCode(() -> new SystemOneQuestion.Choice("q", options(255)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("score 단계가 2~10개 범위를 벗어나면 생성할 때 예외가 발생한다")
  void score_단계_개수를_검증할_수_있다() {
    assertThatThrownBy(() -> new SystemOneQuestion.Score("q", levels(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SystemOneQuestion.Score("q", levels(11)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> new SystemOneQuestion.Score("q", levels(2))).doesNotThrowAnyException();
    assertThatCode(() -> new SystemOneQuestion.Score("q", levels(10))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("instructions가 비어 있으면 생성할 때 예외가 발생한다")
  void instructions_공백을_검증할_수_있다() {
    assertThatThrownBy(() -> new SystemOneQuestion.Noul(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("질문이 없거나 state가 null이면 요청을 생성할 때 예외가 발생한다")
  void request_필수값을_검증할_수_있다() {
    assertThatThrownBy(() -> new SystemOneRequest("state", Collections.emptyMap()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new SystemOneRequest(null, Map.of("k", new SystemOneQuestion.Noul("q"))))
        .isInstanceOf(NullPointerException.class);
  }

  private static Map<String, String> options(int size) {
    return IntStream.range(0, size)
        .boxed()
        .collect(Collectors.toMap(i -> "o" + i, i -> "option " + i));
  }

  private static List<String> levels(int size) {
    return IntStream.range(0, size).mapToObj(i -> "level " + i).toList();
  }
}
