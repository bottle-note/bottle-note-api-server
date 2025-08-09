package app.bottlenote.picks.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PicksStatus {
  PICK,
  UNPICK;

  @JsonCreator
  public static PicksStatus parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return PicksStatus.valueOf(source.toUpperCase());
  }
}
