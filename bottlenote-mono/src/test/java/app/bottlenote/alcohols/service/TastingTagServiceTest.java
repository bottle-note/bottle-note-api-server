package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("TastingTagService 단위 테스트")
class TastingTagServiceTest {

  InMemoryTastingTagRepository tastingTagRepository;
  TastingTagService tastingTagService;

  @BeforeEach
  void setUp() {
    tastingTagRepository = new InMemoryTastingTagRepository();
    tastingTagService = new TastingTagService(tastingTagRepository);

    tastingTagRepository.save(createTag("바닐라", "vanilla"));
    tastingTagRepository.save(createTag("꿀", "honey"));
    tastingTagRepository.save(createTag("스모키", "smoky"));
    tastingTagRepository.save(createTag("피트", "peat"));

    tastingTagService.initializeTrie();
  }

  @Nested
  @DisplayName("extractTagNames 메서드")
  class ExtractTagNames {

    @Test
    @DisplayName("정상적으로 태그를 추출한다")
    void 정상_매칭() {
      // given
      String text = "바닐라 향이 좋아요";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).containsExactly("바닐라");
    }

    @Test
    @DisplayName("여러 태그를 추출한다")
    void 여러_태그_매칭() {
      // given
      String text = "바닐라, 꿀 향이 나고 스모키해요";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).containsExactlyInAnyOrder("바닐라", "꿀");
    }

    @Test
    @DisplayName("부분 매칭은 제외한다")
    void 부분_매칭_제외() {
      // given
      String text = "바닐라빈과 꿀물";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("영문 태그를 추출한다")
    void 영문_매칭() {
      // given
      String text = "vanilla and honey flavor";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).containsExactlyInAnyOrder("vanilla", "honey");
    }

    @Test
    @DisplayName("대소문자를 무시하고 추출한다")
    void 대소문자_무시() {
      // given
      String text = "VANILLA HONEY";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).containsExactlyInAnyOrder("vanilla", "honey");
    }

    @Test
    @DisplayName("중복 태그는 제거한다")
    void 중복_제거() {
      // given
      String text = "바닐라 향과 바닐라 맛";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).containsExactly("바닐라");
    }

    @Test
    @DisplayName("빈 문자열은 빈 리스트를 반환한다")
    void 빈_문자열() {
      // given
      String text = "";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null은 빈 리스트를 반환한다")
    void null_입력() {
      // when
      List<String> result = tastingTagService.extractTagNames(null);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("매칭되는 태그가 없으면 빈 리스트를 반환한다")
    void 매칭_없음() {
      // given
      String text = "아무런 태그도 없는 문장";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).isEmpty();
    }
  }

  private TastingTag createTag(String korName, String engName) {
    return TastingTag.builder().korName(korName).engName(engName).build();
  }
}
