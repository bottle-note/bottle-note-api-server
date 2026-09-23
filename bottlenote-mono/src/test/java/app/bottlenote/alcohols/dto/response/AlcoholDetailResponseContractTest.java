package app.bottlenote.alcohols.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AlcoholDetailResponse 응답 계약")
class AlcoholDetailResponseContractTest {

  @Test
  @DisplayName("Product 알코올 상세에 description 필드를 제공한다")
  void 상세_응답에_description을_제공한다() throws NoSuchFieldException {
    assertThat(Arrays.stream(AlcoholDetailItem.class.getDeclaredFields()).map(Field::getName))
        .contains("description");
  }
}
