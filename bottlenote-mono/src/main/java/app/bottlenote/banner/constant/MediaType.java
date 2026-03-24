package app.bottlenote.banner.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaType {
  IMAGE("이미지"),
  VIDEO("동영상");

  private final String description;

  @JsonCreator
  public static MediaType parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return MediaType.valueOf(source.toUpperCase());
  }
}
