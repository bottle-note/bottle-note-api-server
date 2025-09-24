package app.bottlenote.shared.constant.alcohol;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 특정 키워드에 대한 태그 필터링 규칙 정의 */
@Getter
@RequiredArgsConstructor
public enum KeywordTagMapping {
  SPRING_WHISKEY(
      "봄 추천 위스키",
      List.of(
          1L, 5L, 8L, 9L, 10L, 11L, 14L, 16L, 17L, 23L, 29L, 33L, 35L, 48L, 54L, 60L, 62L, 74L, 75L,
          90L, 92L, 97L, 103L, 107L, 110L, 111L, 117L, 131L, 138L),
      List.of(
          20L, 35L, 50L, 41L, 45L, 46L, 47L, 49L, 63L, 72L, 80L, 84L, 102L, 105L, 118L, 139L, 140L,
          153L, 162L, 167L, 170L, 171L, 172L, 183L)),
  SUMMER_WHISKEY(
      "여름 추천 위스키",
      List.of(
          8L, 9L, 10L, 60L, 76L, 90L, 91L, 93L, 98L, 106L, 110L, 112L, 116L, 119L, 120L, 131L, 134L,
          135L, 136L, 138L, 141L, 142L, 144L, 149L),
      List.of(
          4L, 18L, 63L, 101L, 102L, 105L, 109L, 123L, 124L, 125L, 126L, 159L, 169L, 171L, 183L,
          195L)),

  AUTUMN_WHISKEY(
      "가을 추천 위스키",
      List.of(11L, 12L, 13L, 15L, 30L, 43L, 47L, 84L, 113L, 124L, 125L, 126L, 192L, 196L, 200L),
      List.of(
          1L, 8L, 60L, 62L, 74L, 75L, 90L, 92L, 97L, 107L, 134L, 135L, 138L, 141L, 142L, 144L, 181L,
          197L)),

  WINTER_WHISKEY(
      "겨울 추천 위스키",
      List.of(
          27L, 30L, 31L, 34L, 46L, 63L, 66L, 70L, 72L, 73L, 80L, 84L, 105L, 139L, 169L, 170L, 172L,
          180L, 188L, 195L),
      List.of(
          1L, 8L, 60L, 62L, 74L, 75L, 90L, 92L, 93L, 97L, 103L, 107L, 134L, 135L, 138L, 141L, 142L,
          144L, 181L, 197L, 202L, 203L, 208L)),

  RAINY_DAY_WHISKEY(
      "비 오는 날 추천 위스키",
      List.of(
          24L, 28L, 18L, 19L, 20L, 46L, 50L, 139L, 156L, 164L, 165L, 166L, 167L, 168L, 170L, 171L,
          172L, 188L, 195L, 202L, 203L, 206L),
      List.of(1L, 8L, 74L, 75L, 93L, 97L, 103L, 107L, 134L, 135L, 144L, 197L));

  private final String keyword;
  private final List<Long> includeTags;
  private final List<Long> excludeTags;

  public static Optional<KeywordTagMapping> findByKeyword(String keyword) {
    return Arrays.stream(values()).filter(mapping -> mapping.keyword.equals(keyword)).findFirst();
  }
}
