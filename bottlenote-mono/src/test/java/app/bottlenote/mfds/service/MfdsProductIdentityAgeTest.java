package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("제품 식별 숙성 연수 파서")
class MfdsProductIdentityAgeTest {

  @Test
  @DisplayName("yr 약어와 기존 숙성 표기에서 연수를 읽고 붙은 숫자는 연수로 보지 않는다")
  void 약어와_기존_표기와_경계를_구분한다() {
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12yr")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12 yr")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12YR")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12yrs")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12 YRS")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Glenfiddich 12 year")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Glenfiddich 12 years")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Glenfiddich 12 years old")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Glenfiddich 12yo")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Glenfiddich 12 y")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("글렌피딕 12년")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("몽키숄더 12년산")).isEqualTo(12);
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 2012")).isNull();
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 700ml")).isNull();
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12yellow")).isNull();
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 2012yr")).isNull();
    assertThat(MfdsProductIdentity.parsedAge("Monkey Shoulder 12yr2020")).isNull();
  }
}
