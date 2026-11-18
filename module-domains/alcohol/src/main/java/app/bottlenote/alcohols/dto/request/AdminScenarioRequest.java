package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.domain.ScenarioDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class AdminScenarioRequest {
  private AdminScenarioRequest() {}

  public record Create(
      @NotBlank @Size(max = 100) String title, @NotNull @Min(1) @Max(3) Integer slot, String code) {
    public Create(String title, Integer slot) {
      this(title, slot, null);
    }
  }

  public record Update(
      @NotNull @PositiveOrZero Long expectedLockVersion,
      @NotBlank @Size(max = 100) String title,
      @NotNull Boolean enabled,
      String code) {
    public Update(Long expectedLockVersion, String title, Boolean enabled) {
      this(expectedLockVersion, title, enabled, null);
    }
  }

  public record Draft(
      @NotNull @PositiveOrZero Long expectedScenarioLockVersion, @Positive Long sourceVersionId) {}

  public record Save(
      @NotNull @PositiveOrZero Long expectedLockVersion,
      @NotNull @Valid ScenarioDefinition definition) {}

  public record Publish(
      @NotNull @PositiveOrZero Long expectedScenarioLockVersion,
      @NotNull @PositiveOrZero Long expectedLockVersion) {}
}
