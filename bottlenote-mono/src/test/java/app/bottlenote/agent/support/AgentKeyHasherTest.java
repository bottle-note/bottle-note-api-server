package app.bottlenote.agent.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
@DisplayName("AgentKeyHasher 단위 테스트")
class AgentKeyHasherTest {

  @Test
  @DisplayName("올바른 API Key를 SHA-256 고정 벡터와 동일하게 해시한다")
  void validateAndHash_올바른_API_Key면_고정_해시를_반환한다() {
    String result = AgentKeyHasher.validateAndHash("bn_agent_" + "A".repeat(43));

    assertThat(result)
        .isEqualTo("c46ed383296d0583a361c196c7d1b79c4baec0d0d1ccec96007d2be721a8492f");
  }

  @ParameterizedTest
  @MethodSource("invalidApiKeys")
  @DisplayName("접두사, 길이, 문자집합 또는 공백이 잘못되면 예외를 발생시킨다")
  void validateAndHash_API_Key_형식이_아니면_예외를_발생시킨다(String invalidApiKey) {
    assertThatThrownBy(() -> AgentKeyHasher.validateAndHash(invalidApiKey))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static String[] invalidApiKeys() {
    return new String[] {
      "agent_" + "A".repeat(43),
      "bn_agent_" + "A".repeat(42),
      "bn_agent_" + "A".repeat(42) + "+",
      " bn_agent_" + "A".repeat(43)
    };
  }
}
