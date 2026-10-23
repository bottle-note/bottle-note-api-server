package app.bottlenote.alcohols.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class ScenarioTypes {
  private ScenarioTypes() {}

  public enum Status {
    DRAFT,
    PUBLISHED;

    @JsonCreator
    public static Status parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }

  public enum Input {
    SINGLE_CHOICE,
    NUMBER_RANGE;

    @JsonCreator
    public static Input parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }

  public enum Operator {
    EQ,
    GTE,
    LTE,
    ALWAYS;

    @JsonCreator
    public static Operator parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }

  public enum Target {
    QUESTION,
    END;

    @JsonCreator
    public static Target parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }

  public enum AnswerKind {
    CHOICE,
    NUMBER_RANGE,
    ANY;

    @JsonCreator
    public static AnswerKind parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }
}
