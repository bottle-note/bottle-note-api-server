package app.bottlenote.shared.cursor;

import app.bottlenote.shared.annotation.ExcludeRule;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ExcludeRule
public enum SortOrder {
	DESC("내림차순"),
	ASC("오름차순");

	private final String description;

	@JsonCreator
	public static SortOrder parsing(String source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		return Stream.of(SortOrder.values())
			.filter(sort -> sort.toString().equals(source.toUpperCase()))
			.findFirst()
			.orElse(DESC);
	}
}