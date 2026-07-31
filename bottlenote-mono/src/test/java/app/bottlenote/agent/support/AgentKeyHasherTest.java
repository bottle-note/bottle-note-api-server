package app.bottlenote.agent.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AgentKeyHasher 단위 테스트")
class AgentKeyHasherTest {

  @Test
  @DisplayName("UUID를 정규화한 뒤 SHA-256 고정 벡터와 동일하게 해시한다")
  void normalizeAndHash_대문자와_공백이_있는_UUID_고정_해시를_반환한다() {
    String result = AgentKeyHasher.normalizeAndHash(" ABCDEFAB-CDEF-ABCD-EFAB-CDEFABCDEFAB ");

    assertThat(result)
        .isEqualTo("7676cf41072021ea236fc87b2abf756d540d3365afa7dde043a10c35f385f341");
  }

  @Test
  @DisplayName("UUID 형식이 아니면 예외를 발생시킨다")
  void normalizeAndHash_UUID_형식이_아니면_예외를_발생시킨다() {
    assertThatThrownBy(() -> AgentKeyHasher.normalizeAndHash("not-a-uuid"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
