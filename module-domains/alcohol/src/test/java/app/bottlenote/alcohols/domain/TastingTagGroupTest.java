package app.bottlenote.alcohols.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("맛 그룹 도메인 단위 테스트")
@SuppressWarnings("NonAsciiCharacters")
class TastingTagGroupTest {

  @Test
  @DisplayName("코드·이름·계열을 다듬어 그룹을 만들고 노출 순서는 0이 기본이다")
  void 그룹을_만들_수_있다() {
    TastingTagGroup group = TastingTagGroup.create(" PEAT_SMOKY ", " 피트·스모키 ", " 스카치 ", "", null);

    assertThat(group.getCode()).isEqualTo("PEAT_SMOKY");
    assertThat(group.getName()).isEqualTo("피트·스모키");
    assertThat(group.getFamily()).isEqualTo("스카치");
    assertThat(group.getDescription()).isNull();
    assertThat(group.getDisplayOrder()).isZero();
    assertThat(group.hasTags()).isFalse();
  }

  @ParameterizedTest(name = "code=\"{0}\"")
  @CsvSource({"peat_smoky", "1PEAT", "PEAT-SMOKY", "''"})
  @DisplayName("코드가 영문 대문자·숫자·밑줄 형식이 아니면 거부한다")
  void 잘못된_코드는_거부한다(String code) {
    assertThatThrownBy(() -> TastingTagGroup.create(code, "이름", null, null, 0))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.TASTING_TAG_GROUP_INVALID_CODE.getMessage());
  }

  @Test
  @DisplayName("이름이 비어 있으면 거부한다")
  void 빈_이름은_거부한다() {
    assertThatThrownBy(() -> TastingTagGroup.create("PEAT", "  ", null, null, 0))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.TASTING_TAG_GROUP_INVALID_NAME.getMessage());
  }

  @Test
  @DisplayName("같은 태그를 다시 넣으면 역할만 바뀌고 하나로 남는다")
  void 같은_태그는_역할만_바뀐다() {
    TastingTagGroup group = TastingTagGroup.create("PEAT", "피트", null, null, 0);

    group.putTag(139L, TastingTagGroupRole.PERIPHERAL);
    group.putTag(139L, TastingTagGroupRole.CORE);
    group.putTag(170L, TastingTagGroupRole.PERIPHERAL);

    assertThat(group.getTags()).hasSize(2);
    assertThat(group.findTag(139L))
        .map(TastingTagGroupTag::getRole)
        .contains(TastingTagGroupRole.CORE);
    assertThat(group.coreTagIds()).containsExactly(139L);
  }

  @Test
  @DisplayName("태그를 빼면 true, 없는 태그를 빼면 false를 돌려준다")
  void 태그를_뺄_수_있다() {
    TastingTagGroup group = TastingTagGroup.create("PEAT", "피트", null, null, 0);
    group.putTag(139L, TastingTagGroupRole.CORE);

    assertThat(group.removeTag(139L)).isTrue();
    assertThat(group.removeTag(139L)).isFalse();
    assertThat(group.hasTags()).isFalse();
  }

  @Test
  @DisplayName("연결을 통째로 교체하면 이전 연결은 사라진다")
  void 연결을_교체할_수_있다() {
    TastingTagGroup group = TastingTagGroup.create("PEAT", "피트", null, null, 0);
    group.putTag(139L, TastingTagGroupRole.CORE);

    group.replaceTags(
        List.of(
            TastingTagGroupTag.of(170L, TastingTagGroupRole.CORE),
            TastingTagGroupTag.of(162L, TastingTagGroupRole.PERIPHERAL)));

    assertThat(group.findTag(139L)).isEmpty();
    assertThat(group.coreTagIds()).containsExactly(170L);
    assertThat(group.findTag(162L))
        .map(TastingTagGroupTag::weight)
        .contains(TastingTagGroupRole.PERIPHERAL.weight());
  }

  @Test
  @DisplayName("수정은 코드를 두고 이름·계열·설명·순서만 바꾼다")
  void 수정하면_코드는_유지된다() {
    TastingTagGroup group = TastingTagGroup.create("PEAT", "피트", "스카치", null, 0);

    group.update("피트·스모키", "", "이탄 연기", 3);

    assertThat(group.getCode()).isEqualTo("PEAT");
    assertThat(group.getName()).isEqualTo("피트·스모키");
    assertThat(group.getFamily()).isNull();
    assertThat(group.getDescription()).isEqualTo("이탄 연기");
    assertThat(group.getDisplayOrder()).isEqualTo(3);
  }
}
