package app.bottlenote.user.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdminUserSortType {
  CREATED_AT("가입일"),
  NICK_NAME("닉네임"),
  EMAIL("이메일"),
  RATING_COUNT("별점 많은 순"),
  REVIEW_COUNT("리뷰 많은 순");

  private final String description;

  @JsonCreator
  public static AdminUserSortType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return CREATED_AT;
    }
    return Stream.of(AdminUserSortType.values())
        .filter(sortType -> sortType.toString().equals(source.toUpperCase()))
        .findFirst()
        .orElse(CREATED_AT);
  }
}
