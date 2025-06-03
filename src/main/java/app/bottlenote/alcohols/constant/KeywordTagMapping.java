package app.bottlenote.alcohols.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 특정 키워드에 대한 태그 필터링 규칙 정의
 */
@Getter
@RequiredArgsConstructor
public enum KeywordTagMapping {

	SPRING_WHISKEY(
			"봄 추천 위스키",
			List.of(1L, 5L, 8L, 9L, 10L, 11L, 14L, 16L, 17L, 19L,
					22L, 23L, 29L, 33L, 35L, 42L, 47L, 48L, 54L, 60L,
					62L, 66L, 70L, 72L, 73L, 75L, 80L, 88L, 94L),
			List.of(34L, 41L, 45L, 49L, 63L, 102L, 105L, 118L, 139L, 140L, 153L, 162L, 167L, 170L, 172L)
	),

	SUMMER_WHISKEY("여름 추천 위스키",
			List.of(6L, 7L, 8L, 9L, 10L),
			List.of()),

	AUTUMN_WHISKEY("가을 추천 위스키",
			List.of(11L, 12L, 13L, 14L, 15L),
			List.of()),

	WINTER_WHISKEY("겨울 추천 위스키",
			List.of(16L, 17L, 18L, 19L, 20L),
			List.of()),

	RAINY_DAY_WHISKEY("비 오늘 날 추천 위스키",
			List.of(16L, 17L, 18L, 19L, 20L, 21L),
			List.of());

	private final String keyword;
	private final List<Long> includeTags;
	private final List<Long> excludeTags;

	public static Optional<KeywordTagMapping> findByKeyword(String keyword) {
		return Arrays.stream(values())
				.filter(mapping -> mapping.keyword.equals(keyword))
				.findFirst();
	}
}
