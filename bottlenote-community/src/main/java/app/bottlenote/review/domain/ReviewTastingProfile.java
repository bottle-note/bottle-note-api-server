package app.bottlenote.review.domain;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_PROFILE;

import app.bottlenote.review.exception.ReviewException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReviewTastingProfile(int version, int maxScore, List<Axis> axes) {

  private static final int SUPPORTED_VERSION = 1;
  private static final int MIN_MAX_SCORE = 1;
  private static final int MAX_MAX_SCORE = 100;
  private static final int MIN_AXIS_COUNT = 1;
  private static final int MAX_AXIS_COUNT = 20;
  private static final int MIN_NAME_LENGTH = 1;
  private static final int MAX_NAME_LENGTH = 10;

  public record Axis(String code, String name, String description, int score) {}

  public static ReviewTastingProfile normalize(ReviewTastingProfile profile) {
    if (profile == null) {
      return null;
    }
    if (profile.version != SUPPORTED_VERSION
        || profile.maxScore < MIN_MAX_SCORE
        || profile.maxScore > MAX_MAX_SCORE
        || profile.axes == null
        || profile.axes.size() < MIN_AXIS_COUNT
        || profile.axes.size() > MAX_AXIS_COUNT) {
      throw new ReviewException(INVALID_TASTING_PROFILE);
    }

    List<Axis> axes =
        profile.axes.stream().map(axis -> normalizeAxis(axis, profile.maxScore)).toList();
    if (axes.stream().allMatch(axis -> axis.score == 0)) {
      return null;
    }
    Set<String> names = new LinkedHashSet<>();
    for (Axis axis : axes) {
      if (!names.add(axis.name)) {
        throw new ReviewException(INVALID_TASTING_PROFILE);
      }
    }
    return new ReviewTastingProfile(profile.version, profile.maxScore, List.copyOf(axes));
  }

  private static Axis normalizeAxis(Axis axis, int maxScore) {
    if (axis == null || axis.score < 0 || axis.score > maxScore) {
      throw new ReviewException(INVALID_TASTING_PROFILE);
    }
    String name = axis.name == null ? "" : axis.name.trim();
    if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
      throw new ReviewException(INVALID_TASTING_PROFILE);
    }
    String description = blankToNull(axis.description);
    String code = blankToNull(axis.code);
    return new Axis(code, name, description, axis.score);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReviewTastingProfile profile)) {
      return false;
    }
    return version == profile.version
        && maxScore == profile.maxScore
        && Objects.equals(axes, profile.axes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, maxScore, axes);
  }
}
