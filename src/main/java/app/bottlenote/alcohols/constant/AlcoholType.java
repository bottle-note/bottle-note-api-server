package app.bottlenote.alcohols.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum AlcoholType {
	WHISKY("위스키", "위스키", "Whisky", "기본 위스키", "Default Whisky"),
	RUM("럼", "럼", "Rum", "기본 럼", "Default Rum"),
	VODKA("보드카", "보드카", "Vodka", "기본 보드카", "Default Vodka"),
	GIN("진", "진", "Gin", "기본 진", "Default Gin"),
	TEQUILA("데킬라", "데킬라", "Tequila", "기본 데킬라", "Default Tequila"),
	BRANDY("브랜디", "브랜디", "Brandy", "기본 브랜디", "Default Brandy"),
	BEER("맥주", "맥주", "Beer", "기본 맥주", "Default Beer"),
	WINE("와인", "와인", "Wine", "기본 와인", "Default Wine"),
	ETC("기타", "기타", "Others", "기타 술", "Default Alcohol");

	private final String type;
	private final String korCategory;
	private final String engCategory;
	private final String defaultKorName;
	private final String defaultEngName;

	@JsonCreator
	public static AlcoholType parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(AlcoholType.values())
			.filter(sortType -> sortType.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(WHISKY);
	}

	/**
	 * 해당 타입의 기본 카테고리 그룹 반환
	 */
	public AlcoholCategoryGroup getDefaultCategoryGroup() {
		return this == WHISKY ? AlcoholCategoryGroup.SINGLE_MALT : AlcoholCategoryGroup.OTHER;
	}
}
