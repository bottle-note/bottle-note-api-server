package app.bottlenote.curation.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationSpecFingerprint 단위 테스트")
class CurationSpecFingerprintTest {

  private final CurationSpecFingerprint fingerprint =
      new CurationSpecFingerprint(new ObjectMapper());

  @Test
  @DisplayName("최상위와 중첩 객체의 키 순서가 달라도 같은 지문이다")
  void isSame_whenKeyOrderDiffers_treatsAsSame() {
    Map<String, Object> left = map("b", map("y", 1, "x", 2), "a", 3);
    Map<String, Object> right = map("a", 3, "b", map("x", 2, "y", 1));

    assertThat(fingerprint.isSame(left, right)).isTrue();
  }

  @Test
  @DisplayName("배열 안 객체의 키 순서가 달라도 같은 지문이다")
  void isSame_whenKeyOrderDiffersInsideArray_treatsAsSame() {
    Map<String, Object> left = map("items", List.of(map("role", "title", "enabled", true)));
    Map<String, Object> right = map("items", List.of(map("enabled", true, "role", "title")));

    assertThat(fingerprint.isSame(left, right)).isTrue();
  }

  @Test
  @DisplayName("배열 원소 순서가 다르면 다른 지문이다")
  void isSame_whenArrayOrderDiffers_treatsAsDifferent() {
    Map<String, Object> left = map("required", List.of("source", "alcohol"));
    Map<String, Object> right = map("required", List.of("alcohol", "source"));

    assertThat(fingerprint.isSame(left, right)).isFalse();
  }

  @Test
  @DisplayName("정수와 정수로 표현 가능한 실수는 같은 지문이다")
  void isSame_whenIntegerAndWholeDecimal_treatsAsSame() {
    Map<String, Object> left = map("order", 1);
    Map<String, Object> right = map("order", 1.0);

    assertThat(fingerprint.isSame(left, right)).isTrue();
  }

  @Test
  @DisplayName("실수의 trailing zero 차이는 같은 지문이다")
  void isSame_whenTrailingZeroDiffers_treatsAsSame() {
    Map<String, Object> left = map("abv", new BigDecimal("1.20"));
    Map<String, Object> right = map("abv", new BigDecimal("1.2"));

    assertThat(fingerprint.isSame(left, right)).isTrue();
  }

  @Test
  @DisplayName("null 값과 키 부재는 다른 지문이다")
  void isSame_whenNullValueVersusAbsentKey_treatsAsDifferent() {
    Map<String, Object> withNull = map("nullable", null);
    Map<String, Object> withoutKey = map();

    assertThat(fingerprint.isSame(withNull, withoutKey)).isFalse();
  }

  @Test
  @DisplayName("빈 객체와 빈 배열은 다른 지문이다")
  void isSame_whenEmptyObjectVersusEmptyArray_treatsAsDifferent() {
    assertThat(fingerprint.isSame(map("properties", map()), map("properties", List.of())))
        .isFalse();
  }

  @Test
  @DisplayName("값이 하나라도 바뀌면 다른 지문이다")
  void isSame_whenValueChanges_treatsAsDifferent() {
    Map<String, Object> left = map("x-feed", map("enabled", true));
    Map<String, Object> right = map("x-feed", map("enabled", false));

    assertThat(fingerprint.isSame(left, right)).isFalse();
  }

  @Test
  @DisplayName("같은 입력에 대해 지문이 항상 같다")
  void of_whenSameInput_isStable() {
    Map<String, Object> spec = map("a", map("b", List.of(1, map("c", "d"))));

    assertThat(fingerprint.of(spec)).isEqualTo(fingerprint.of(spec));
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
