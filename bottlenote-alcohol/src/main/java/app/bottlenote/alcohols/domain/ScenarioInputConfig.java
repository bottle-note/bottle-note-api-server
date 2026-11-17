package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.exception.ScenarioException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import java.util.List;

public record ScenarioInputConfig(
    @JsonDeserialize(using = ScenarioJson.Bool.class) Boolean allowAny,
    List<ScenarioChoice> choices,
    @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal min,
    @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal max,
    @JsonDeserialize(using = ScenarioJson.Decimal.class) BigDecimal step,
    String unit) {
  public List<String> choiceCodes() {
    if (choices == null) return List.of();
    return choices.stream()
        .filter(java.util.Objects::nonNull)
        .filter(c -> c.value() != null && c.value().isTextual())
        .map(c -> c.value().textValue())
        .toList();
  }

  @JsonAnySetter
  public void rejectUnknown(String key, JsonNode value) {
    throw ScenarioException.invalid("inputConfig에 미지원 필드가 있습니다: " + key);
  }

  public record ScenarioChoice(String id, String label, JsonNode value) {
    @JsonAnySetter
    public void rejectUnknown(String key, JsonNode value) {
      throw ScenarioException.invalid("선택지에 미지원 필드가 있습니다: " + key);
    }
  }
}
