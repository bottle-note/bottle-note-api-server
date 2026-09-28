package app.bottlenote.alcohols.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("주류 타입")
class AlcoholTypeTest {

  @Test
  @DisplayName("리큐르 타입의 표시값과 기본 그룹을 제공한다")
  void 리큐르_타입을_제공한다() {
    AlcoholType liqueur = AlcoholType.LIQUEUR;

    assertThat(liqueur.getType()).isEqualTo("리큐르");
    assertThat(liqueur.getKorCategory()).isEqualTo("리큐르");
    assertThat(liqueur.getEngCategory()).isEqualTo("Liqueur");
    assertThat(liqueur.getDefaultKorName()).isEqualTo("기본 리큐르");
    assertThat(liqueur.getDefaultEngName()).isEqualTo("Default Liqueur");
    assertThat(liqueur.getDefaultCategoryGroup()).isEqualTo(AlcoholCategoryGroup.OTHER);
  }

  @Test
  @DisplayName("리큐르 enum 코드를 대소문자와 무관하게 파싱한다")
  void 리큐르_코드를_파싱한다() {
    assertThat(AlcoholType.parsing("LIQUEUR")).isEqualTo(AlcoholType.LIQUEUR);
    assertThat(AlcoholType.parsing("liqueur")).isEqualTo(AlcoholType.LIQUEUR);
  }
}
