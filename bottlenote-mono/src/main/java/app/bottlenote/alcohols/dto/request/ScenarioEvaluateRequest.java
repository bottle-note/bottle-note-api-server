package app.bottlenote.alcohols.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ScenarioEvaluateRequest(
    @NotNull @Positive Long versionId,
    @NotNull @Size(max = 100) List<@NotNull @Valid ScenarioSubmittedAnswer> answers,
    @NotNull Mode mode,
    @Positive Long viewQuestionId,
    @Size(max = 4096) String cursor,
    @Min(1) @Max(100) Integer size) {
  public ScenarioEvaluateRequest {
    size = size == null ? 20 : size;
  }

  public enum Mode {
    QUESTION,
    RESULT;

    @JsonCreator
    public static Mode parse(String value) {
      return value == null ? null : valueOf(value);
    }
  }

  public record ScenarioSubmittedAnswer(
      @NotNull @Positive Long questionId, @NotBlank @Size(max = 2048) String optionId) {}
}
