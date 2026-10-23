package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import app.bottlenote.alcohols.domain.ScenarioJson;
import app.bottlenote.alcohols.exception.ScenarioException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
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

  public record ScenarioAnswer(
      @NotNull @Positive Long questionId,
      @NotNull AnswerKind kind,
      String choiceId,
      @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal min,
      @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal max) {
    @JsonAnySetter
    public void rejectUnknown(String key, JsonNode value) {
      throw ScenarioException.invalid("답변에 미지원 필드가 있습니다: " + key);
    }
  }
}
