package app.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.service.meta.MetaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] 오류 응답 예시")
class ErrorResponseSchemaTest {

  @Test
  @DisplayName("meta 예시의 키는 실제 메타 정보의 키와 같다")
  void meta_예시의_키가_실제와_같다() {
    var actual = MetaService.createMetaInfo().getMetaInfos().keySet();

    assertThat(ErrorResponseSchema.EXAMPLE_META.keySet())
        .withFailMessage(
            "문서의 meta 예시가 실제 메타 정보와 어긋납니다. 예시 %s, 실제 %s",
            ErrorResponseSchema.EXAMPLE_META.keySet(), actual)
        .containsExactlyInAnyOrderElementsOf(actual);
  }
}
