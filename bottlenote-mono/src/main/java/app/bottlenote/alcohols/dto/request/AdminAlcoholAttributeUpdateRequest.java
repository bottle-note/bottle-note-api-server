package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;

public final class AdminAlcoholAttributeUpdateRequest {
  @JsonProperty
  @NotBlank(message = "ALCOHOL_ATTRIBUTE_NAME_REQUIRED")
  @Size(max = 100, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
  private final String displayName;

  @JsonProperty
  @NotNull(message = "ALCOHOL_ATTRIBUTE_TYPE_REQUIRED")
  private final AttributeType type;

  @JsonProperty
  @NotNull(message = "ALCOHOL_ATTRIBUTE_DATATYPE_REQUIRED")
  private final AttributeDatatype datatype;

  @JsonProperty
  @Size(max = 20, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
  private final String unit;

  @JsonProperty
  @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
  private final String description;

  @Positive(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
  @JsonPropertyDescription("소속 그룹 ID. 생략하면 기존 소속 유지, null이면 소속 해제")
  private Long groupId;

  private boolean groupIdPresent;

  @JsonCreator
  public AdminAlcoholAttributeUpdateRequest(
      @JsonProperty("displayName") String displayName,
      @JsonProperty("type") AttributeType type,
      @JsonProperty("datatype") AttributeDatatype datatype,
      @JsonProperty("unit") String unit,
      @JsonProperty("description") String description) {
    this.displayName = displayName;
    this.type = type;
    this.datatype = datatype;
    this.unit = unit;
    this.description = description;
  }

  public String displayName() {
    return displayName;
  }

  public AttributeType type() {
    return type;
  }

  public AttributeDatatype datatype() {
    return datatype;
  }

  public String unit() {
    return unit;
  }

  public String description() {
    return description;
  }

  @JsonGetter("groupId")
  public Long groupId() {
    return groupId;
  }

  @JsonSetter("groupId")
  public void setGroupId(Long groupId) {
    this.groupId = groupId;
    this.groupIdPresent = true;
  }

  @JsonIgnore
  public boolean groupIdPresent() {
    return groupIdPresent;
  }
}
