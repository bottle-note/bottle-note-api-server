package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.domain.ScenarioAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AdminScenarioPreviewRequest(
    @NotNull @PositiveOrZero Long expectedLockVersion,
    @NotNull @Size(max = 100) List<@NotNull @Valid ScenarioAnswer> answers,
    @Positive Long changedQuestionId,
    @Positive Long viewQuestionId,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size) {
  public AdminScenarioPreviewRequest {
    page = page == null ? 0 : page;
    size = size == null ? 25 : size;
  }
}
