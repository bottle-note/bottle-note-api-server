package app.bottlenote.alcohols.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AlcoholQuestionSource {
  ATTRIBUTE,
  CATEGORY,
  TASTING_GROUP,
  ABV;

  @JsonCreator
  public static AlcoholQuestionSource parse(String value) {
    return value == null ? null : valueOf(value);
  }
}
