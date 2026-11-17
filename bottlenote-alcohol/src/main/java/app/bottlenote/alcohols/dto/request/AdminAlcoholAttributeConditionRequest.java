package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAlcoholAttributeConditionRequest(
    @NotBlank(message = "ALCOHOL_ATTRIBUTE_KEY_INVALID")
        @Pattern(regexp = "[a-z][a-z0-9_]{0,63}", message = "ALCOHOL_ATTRIBUTE_KEY_INVALID")
        String key,
    AttributeSearchOperator operator,
    @Size(max = 255, message = "ALCOHOL_ATTRIBUTE_INVALID_VALUE") String value) {
  public AdminAlcoholAttributeConditionRequest {
    key = key == null ? null : key.trim();
    operator = operator == null ? AttributeSearchOperator.EQ : operator;
    value = value == null || value.isBlank() ? null : value.trim();
  }
}
