package app.bottlenote.like.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LikeStatus {
  LIKE,
  DISLIKE;

  @JsonCreator
  public static LikeStatus parsing(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return LikeStatus.valueOf(source.toUpperCase());
  }
}
