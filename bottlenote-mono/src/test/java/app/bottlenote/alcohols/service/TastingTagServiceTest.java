package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_MAPPING_DUPLICATE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.TASTING_TAG_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholsTastingTags;
import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository;
import app.bottlenote.alcohols.fixture.InMemoryAlcoholsTastingTagsRepository;
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
  InMemoryAlcoholsTastingTagsRepository alcoholsTastingTagsRepository;
  InMemoryAlcoholQueryRepository alcoholQueryRepository;
  TastingTagService tastingTagService;

  @BeforeEach
  void setUp() {
    tastingTagRepository = new InMemoryTastingTagRepository();
    alcoholsTastingTagsRepository = new InMemoryAlcoholsTastingTagsRepository();
    alcoholQueryRepository = new InMemoryAlcoholQueryRepository();
    tastingTagService =
        new TastingTagService(
            tastingTagRepository, alcoholsTastingTagsRepository, alcoholQueryRepository);

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

  @Nested
  @DisplayName("addAlcoholsToTag 메서드")
  class AddAlcoholsToTag {

    @Test
    @DisplayName("요청 alcoholId 중복은 한 번만 저장한다")
    void 요청_중복_alcoholId는_한_번만_저장한다() {
      TastingTag tag = tastingTagRepository.findByKorName("오크").orElseThrow();
      Alcohol alcohol = alcoholQueryRepository.save(Alcohol.builder().korName("위스키1").build());

      tastingTagService.addAlcoholsToTag(tag.getId(), List.of(alcohol.getId(), alcohol.getId()));

      assertThat(alcoholsTastingTagsRepository.count()).isEqualTo(1);
      assertThat(alcoholsTastingTagsRepository.findAlcoholIdsByTastingTagId(tag.getId()))
          .containsExactly(alcohol.getId());
    }

    @Test
    @DisplayName("이미 연결된 주류는 skip 하여 멱등적으로 성공한다")
    void 이미_연결된_주류는_skip한다() {
      TastingTag tag = tastingTagRepository.findByKorName("오크").orElseThrow();
      Alcohol alcohol = alcoholQueryRepository.save(Alcohol.builder().korName("위스키1").build());
      alcoholsTastingTagsRepository.saveAll(List.of(AlcoholsTastingTags.of(alcohol, tag)));

      tastingTagService.addAlcoholsToTag(tag.getId(), List.of(alcohol.getId()));

      assertThat(alcoholsTastingTagsRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 태그면 TASTING_TAG_NOT_FOUND 예외를 던진다")
    void 태그_미존재_예외() {
      assertThatThrownBy(() -> tastingTagService.addAlcoholsToTag(999L, List.of(1L)))
          .isInstanceOf(AlcoholException.class)
          .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
          .isEqualTo(TASTING_TAG_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 주류면 ALCOHOL_NOT_FOUND 예외를 던진다")
    void 주류_미존재_예외() {
      TastingTag tag = tastingTagRepository.findByKorName("오크").orElseThrow();

      assertThatThrownBy(() -> tastingTagService.addAlcoholsToTag(tag.getId(), List.of(999L)))
          .isInstanceOf(AlcoholException.class)
          .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
          .isEqualTo(ALCOHOL_NOT_FOUND);
    }

    @Test
    @DisplayName("유니크 제약 위반 시 TASTING_TAG_MAPPING_DUPLICATE 로 변환한다")
    void 유니크_위반_시_매핑_중복_예외() {
      TastingTag tag = tastingTagRepository.findByKorName("오크").orElseThrow();
      Alcohol alcohol = alcoholQueryRepository.save(Alcohol.builder().korName("위스키1").build());
      alcoholsTastingTagsRepository.saveAll(List.of(AlcoholsTastingTags.of(alcohol, tag)));

      // 사전 skip을 우회해 저장 단계에서 중복이 나도록, 기존 매핑 조회 결과를 비우는 대신
      // 동일 매핑을 강제로 한 번 더 저장 시도한다.
      assertThatThrownBy(
              () ->
                  alcoholsTastingTagsRepository.saveAll(
                      List.of(AlcoholsTastingTags.of(alcohol, tag))))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

      // 서비스 계층 변환: 동시성으로 사전 체크가 빈 사이 중복 insert 가 발생한 경우를 모사
      InMemoryAlcoholsTastingTagsRepository racingRepo =
          new InMemoryAlcoholsTastingTagsRepository() {
            @Override
            public java.util.Set<Long> findAlcoholIdsByTastingTagId(Long tastingTagId) {
              return java.util.Set.of();
            }

            @Override
            public <S extends AlcoholsTastingTags> List<S> saveAll(
                Iterable<S> alcoholsTastingTags) {
              throw new org.springframework.dao.DataIntegrityViolationException("duplicate");
            }
          };
      TastingTagService racingService =
          new TastingTagService(tastingTagRepository, racingRepo, alcoholQueryRepository);

      assertThatThrownBy(
              () -> racingService.addAlcoholsToTag(tag.getId(), List.of(alcohol.getId())))
          .isInstanceOf(AlcoholException.class)
          .extracting(ex -> ((AlcoholException) ex).getExceptionCode())
          .isEqualTo(TASTING_TAG_MAPPING_DUPLICATE);
    }
  }

  private TastingTag createTag(String korName, String engName) {
    return TastingTag.builder().korName(korName).engName(engName).build();
  }
}
