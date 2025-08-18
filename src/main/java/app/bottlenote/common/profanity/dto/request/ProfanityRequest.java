package app.bottlenote.common.profanity.dto.request;

import app.bottlenote.common.profanity.constant.FilterMode;

import java.util.Objects;

public record ProfanityRequest(String text, FilterMode mode) {
  public static ProfanityRequest createFilter(String text) {
    Objects.requireNonNull(text, "text must be provided");
    return new ProfanityRequest(text, FilterMode.FILTER);
  }
}
