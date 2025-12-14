package app.bottlenote.alcohols.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminAlcoholSortType {
  KOR_NAME("한글 이름"),
  ENG_NAME("영어 이름"),
  KOR_CATEGORY("카테고리 한글"),
  ENG_CATEGORY("카테고리 영어");

  private final String description;

  @JsonCreator
  public static AdminAlcoholSortType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return KOR_NAME;
    }
    return Stream.of(AdminAlcoholSortType.values())
        .filter(sortType -> sortType.toString().equals(source.toUpperCase()))
        .findFirst()
        .orElse(KOR_NAME);
  }
}
