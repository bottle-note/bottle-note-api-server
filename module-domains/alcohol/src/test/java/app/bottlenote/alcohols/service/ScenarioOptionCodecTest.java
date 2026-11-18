package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import app.bottlenote.alcohols.domain.ScenarioAnswer;
import app.bottlenote.alcohols.domain.ScenarioAnswer.Selection;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.global.pagination.CursorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;

@Tag("unit")
@DisplayName("선택지 서명을 검증할 때 조건과 선행 답변을 보존한다")
class ScenarioOptionCodecTest {
  private final CursorProperties keys = new CursorProperties();
  private final ScenarioOptionCodec codec = new ScenarioOptionCodec(keys, new ObjectMapper());
  private final Selection selection =
      new Selection(
          new ScenarioAnswer(
              11L, AnswerKind.NUMBER_RANGE, null, new BigDecimal("13000"), new BigDecimal("15000")),
          true,
          "13000원 초과 ~ 15000원 이하");

  @BeforeEach
  void setup() {
    keys.setCurrentKeyId("v1");
    keys.setCurrentSecret("unit-test-key-with-sufficient-length");
  }

  @Test
  @DisplayName("내부 타입으로 옮긴 뒤에도 기존 형식으로 발급한 선택지를 읽는다")
  void readsPreviouslyIssuedOption() {
    String token =
        "djE.eyJmb3JtYXQiOjEsImNvbnRleHQiOiJhMGJhNmY1NDc2ZWQyODY3MGYxZTkyOTI3N2E5MjMyNGQ0NTVhOTkzNjA0MGQ2OGZi"
            + "YTk5NWIyMTM3YzZiMDRiIiwic2VsZWN0aW9uIjp7ImFuc3dlciI6eyJxdWVzdGlvbklkIjoxMSwia2luZCI6Ik5VTUJFUl9SQU5H"
            + "RSIsImNob2ljZUlkIjpudWxsLCJtaW4iOjEzMDAwLCJtYXgiOjE1MDAwfSwibWluRXhjbHVzaXZlIjp0cnVlLCJsYWJlbCI6IjEz"
            + "MDAw7JuQIOy0iOqzvCB-IDE1MDAw7JuQIOydtO2VmCJ9fQ.nKsE8r_SqGrtqfcb1CzBdNdxJjbnWCsCGai5viRSmGI";
    assertThat(codec.decode(token, codec.context(1L, 2L, List.of()), 11L)).isEqualTo(selection);
  }

  @Test
  @DisplayName("시간 제한 없이 저장한 선택지를 읽을 때 배타 경계와 문구를 복원한다")
  void preservesPredicateWithoutCursorExpiry() {
    String context = codec.context(1L, 2L, List.of());
    assertThat(codec.decode(codec.encode(context, selection), context, 11L)).isEqualTo(selection);
  }

  @Test
  @DisplayName("시나리오나 버전 또는 질문이나 선행 답변이 달라지면 거절한다")
  void bindsToScenarioVersionQuestionAndPrefix() {
    String context = codec.context(1L, 2L, List.of());
    String token = codec.encode(context, selection);
    for (String other :
        List.of(
            codec.context(2L, 2L, List.of()),
            codec.context(1L, 3L, List.of()),
            codec.context(1L, 2L, List.of(selection)))) {
      assertThatThrownBy(() -> codec.decode(token, other, 11L))
          .isInstanceOf(ScenarioException.class);
    }
    assertThatThrownBy(() -> codec.decode(token, context, 12L))
        .isInstanceOf(ScenarioException.class);
    assertThatThrownBy(() -> codec.decode(token + "a", context, 11L))
        .isInstanceOf(ScenarioException.class);
  }

  @Test
  @DisplayName("키를 교체할 때 이전 키는 허용하고 폐기한 키는 재선택을 요구한다")
  void respectsKeyRotation() {
    String context = codec.context(1L, 2L, List.of());
    String token = codec.encode(context, selection);
    keys.setPreviousKeyId(keys.getCurrentKeyId());
    keys.setPreviousSecret(keys.getCurrentSecret());
    keys.setCurrentKeyId("v2");
    keys.setCurrentSecret("rotated-test-key-with-sufficient-length");
    assertThat(codec.decode(token, context, 11L)).isEqualTo(selection);
    assertThat(codec.decode(codec.encode(context, selection), context, 11L)).isEqualTo(selection);
    keys.setPreviousKeyId(null);
    keys.setPreviousSecret(null);
    assertThatThrownBy(() -> codec.decode(token, context, 11L))
        .isInstanceOf(ScenarioException.class);
  }

  @Test
  @DisplayName("소수 표기만 다른 답변은 같은 조건으로 정규화한다")
  void normalizesDecimalScale() {
    var scaled =
        new Selection(
            new ScenarioAnswer(
                11L,
                AnswerKind.NUMBER_RANGE,
                null,
                new BigDecimal("13000.00"),
                new BigDecimal("15000.0")),
            true,
            selection.label());
    assertThat(codec.context(1L, 2L, List.of(scaled)))
        .isEqualTo(codec.context(1L, 2L, List.of(selection)));
  }

  @Test
  @DisplayName("문구만 달라지면 선행 답변 조건은 같은 문맥으로 평가한다")
  void contextUsesPredicatesRatherThanLabels() {
    var renamed = new Selection(selection.answer(), selection.minExclusive(), "다른 문구");
    assertThat(codec.context(1L, 2L, List.of(selection)))
        .isEqualTo(codec.context(1L, 2L, List.of(renamed)));
    assertThat(codec.context(1L, 2L, List.of(selection)))
        .isNotEqualTo(
            codec.context(
                1L, 2L, List.of(new Selection(selection.answer(), false, selection.label()))));
  }
}
