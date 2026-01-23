package app.bottlenote.alcohols.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

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
    tastingTagRepository.save(createTag("오크", "oak"));
    tastingTagRepository.save(createTag("카라멜", "caramel"));
    tastingTagRepository.save(createTag("시트러스", "citrus"));
    tastingTagRepository.save(createTag("초콜릿", "chocolate"));

    tastingTagService.initializeTrie();
  }

  @Nested
  @DisplayName("extractTagNames 메서드")
  class ExtractTagNames {

    static Stream<Arguments> 리뷰_문장_테스트_케이스() {
      return Stream.of(
          Arguments.of("바닐라 향이 은은하게 퍼지면서 꿀 같은 단맛이 느껴져요", List.of("바닐라", "꿀")),
          Arguments.of("스모키 하면서도 피트 향이 강렬한 아일라 위스키입니다", List.of("스모키", "피트")),
          Arguments.of("오크 통 숙성의 깊은 맛과 카라멜 풍미가 일품이에요", List.of("오크", "카라멜")),
          Arguments.of("입안에서 시트러스 향이 톡 터지고 바닐라 피니시가 길게 이어집니다", List.of("시트러스", "바닐라")),
          Arguments.of("초콜릿, 꿀, 바닐라 삼박자가 완벽한 밸런스를 이룹니다", List.of("초콜릿", "꿀", "바닐라")),
          Arguments.of(
              "This whisky has a nice vanilla and honey sweetness with a hint of oak",
              List.of("vanilla", "honey", "oak")),
          Arguments.of(
              "Smoky peat flavor with caramel undertones", List.of("smoky", "peat", "caramel")),
          Arguments.of("달콤한 꿀 향 뒤로 은은한 스모키 함이 느껴지는 복합적인 위스키", List.of("꿀", "스모키")),
          Arguments.of("첫 모금에 바닐라, 중반에 오크, 피니시에 카라멜 - 완벽한 3단 변화", List.of("바닐라", "오크", "카라멜")),
          Arguments.of("가격 대비 훌륭해요. 시트러스 향과 꿀 맛의 조화가 좋습니다", List.of("시트러스", "꿀")));
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @MethodSource("리뷰_문장_테스트_케이스")
    @DisplayName("리뷰 문장에서 태그를 추출한다")
    void 리뷰_문장에서_태그_추출(String review, List<String> expectedTags) {
      // when
      List<String> result = tastingTagService.extractTagNames(review);

      // then
      assertThat(result).containsExactlyInAnyOrderElementsOf(expectedTags);
    }

    static Stream<Arguments> 부분_매칭_허용_케이스() {
      return Stream.of(
          Arguments.of("바닐라빈 향이 좋아요", List.of("바닐라")),
          Arguments.of("꿀물처럼 달콤해요", List.of("꿀")),
          Arguments.of("스모키한 느낌", List.of("스모키")),
          Arguments.of("카라멜라이즈된 설탕 맛", List.of("카라멜")),
          Arguments.of("초콜릿케이크 같은 맛", List.of("초콜릿")),
          Arguments.of("초콜릿향이 남니다", List.of("초콜릿")));
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @MethodSource("부분_매칭_허용_케이스")
    @DisplayName("부분 매칭을 허용한다")
    void 부분_매칭_허용(String text, List<String> expectedTags) {
      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).containsExactlyInAnyOrderElementsOf(expectedTags);
    }

    @Test
    @DisplayName("중복 태그는 제거한다")
    void 중복_제거() {
      // given
      String text = "바닐라 향과 바닐라 맛이 바닐라 피니시로 이어져요";

      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).hasSize(1);
      assertThat(result).containsExactly("바닐라");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 또는 빈 문자열은 빈 리스트를 반환한다")
    void null_또는_빈_문자열(String text) {
      // when
      List<String> result = tastingTagService.extractTagNames(text);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("매칭되는 태그가 없으면 빈 리스트를 반환한다")
    void 매칭_없음() {
      // given
      String text = "그냥 평범한 위스키입니다. 특별한 향은 못 느꼈어요.";

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
