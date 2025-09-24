package app.bottlenote.shared.constant.alcohol;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchSortType {
  POPULAR("인기순"),
  RATING("별점순"),
  PICK("찜순"),
  REVIEW("리뷰순");

  private final String name;

  @JsonCreator
  public static SearchSortType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return Stream.of(SearchSortType.values())
        .filter(sortType -> sortType.toString().equals(source.toUpperCase()))
        .findFirst()
        .orElse(POPULAR);
  }
}
