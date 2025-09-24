package app.bottlenote.shared.review.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public enum SizeType {
  GLASS,
  BOTTLE;

  @JsonCreator
  public static SizeType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return SizeType.valueOf(source.toUpperCase());
  }
}
