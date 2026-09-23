package app.bottlenote.alcohols.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AlcoholDetailItem OpenAPI 계약")
class AlcoholDetailItemOpenApiTest {

  @Test
  @DisplayName("description은 nullable string으로 문서화한다")
  void description은_nullable_string이다() {
    Schema<?> schema =
        ModelConverters.getInstance()
            .resolveAsResolvedSchema(new AnnotatedType(AlcoholDetailItem.class))
            .schema;
    Schema<?> description = (Schema<?>) schema.getProperties().get("description");

    assertThat(description.getType()).isEqualTo("string");
    assertThat(description.getNullable() == Boolean.TRUE || hasNullType(description)).isTrue();
  }

  private boolean hasNullType(Schema<?> schema) {
    Set<String> types = schema.getTypes();
    return types != null && types.contains("null");
  }
}
