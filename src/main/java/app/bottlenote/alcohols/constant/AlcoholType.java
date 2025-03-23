package app.bottlenote.alcohols.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum AlcoholType {
	WHISKY("위스키"),
	RUM("럼"),
	VODKA("보드카"),
	GIN("진"),
	TEQUILA("데킬라"),
	BRANDY("브랜디"),
	BEER("맥주"),
	WINE("와인"),
	ETC("기타");

	private final String type;

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
}
