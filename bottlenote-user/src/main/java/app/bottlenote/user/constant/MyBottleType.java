package app.bottlenote.user.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MyBottleType {
  REVIEW("리뷰"),
  PICK("찜하기"),
  RATING("별점");

  private final String name;

  @JsonCreator
  public static MyBottleType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return Stream.of(MyBottleType.values())
        .filter(tabType -> tabType.toString().equals(source.toUpperCase()))
        .findFirst()
        .orElse(null);
  }
}
