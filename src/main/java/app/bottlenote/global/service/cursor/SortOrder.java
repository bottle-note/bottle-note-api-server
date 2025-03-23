package app.bottlenote.global.service.cursor;

import app.bottlenote.global.annotation.ExcludeRule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
@ExcludeRule
public enum SortOrder {
	DESC("내림차순"), // 5 => 4.5 => 4 => 1
	ASC("오름차순");  // 1 => 4 => 4.5 => 5

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

	public <T extends Comparable<?>> OrderSpecifier<T> resolve(ComparableExpressionBase<T> expression) {
		return this == DESC ? expression.desc() : expression.asc();
	}
}
