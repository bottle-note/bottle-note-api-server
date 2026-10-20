package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.util.List;

public final class AdminAlcoholAttributeCreateRequest {
  @JsonProperty
  @NotBlank(message = "ALCOHOL_ATTRIBUTE_KEY_REQUIRED")
  @Size(max = 64, message = "ALCOHOL_ATTRIBUTE_FIELD_LENGTH_INVALID")
  private final String key;

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

  @JsonPropertyDescription(
      "STRING 선택값 목록. 최대 100개, 값당 255자. 생략은 기존 규칙 유지, null은 제한 해제. 빈 목록은 허용하지 않음")
  @Size(min = 1, max = 100, message = "ALCOHOL_ATTRIBUTE_INVALID_RULE")
  private List<
          @NotBlank(message = "ALCOHOL_ATTRIBUTE_INVALID_RULE")
          @Size(max = 255, message = "ALCOHOL_ATTRIBUTE_INVALID_RULE") String>
      allowedValues;

  @JsonPropertyDescription(
      "STRING 전체 값에 적용하는 Java 정규식. 최대 500자. 생략은 기존 규칙 유지, null은 제한 해제. allowedValues와 함께 설정하면 모두 만족해야 함")
  @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_INVALID_RULE")
  private String pattern;

  private boolean allowedValuesPresent;
  private boolean patternPresent;

  @JsonGetter("allowedValues")
  public List<String> allowedValues() {
    return allowedValues;
  }

  @JsonSetter("allowedValues")
  public void setAllowedValues(List<String> allowedValues) {
    this.allowedValues = allowedValues;
    this.allowedValuesPresent = true;
  }

  @JsonGetter("pattern")
  public String pattern() {
    return pattern;
  }

  @JsonSetter("pattern")
  public void setPattern(String pattern) {
    this.pattern = pattern;
    this.patternPresent = true;
  }

  @JsonIgnore
  public boolean allowedValuesPresent() {
    return allowedValuesPresent;
  }

  @JsonIgnore
  public boolean patternPresent() {
    return patternPresent;
  }

  private boolean groupIdPresent;

  @JsonCreator
  public AdminAlcoholAttributeCreateRequest(
      @JsonProperty("key") String key,
      @JsonProperty("displayName") String displayName,
      @JsonProperty("type") AttributeType type,
      @JsonProperty("datatype") AttributeDatatype datatype,
      @JsonProperty("unit") String unit,
      @JsonProperty("description") String description) {
    this.key = key;
    this.displayName = displayName;
    this.type = type;
    this.datatype = datatype;
    this.unit = unit;
    this.description = description;
  }

  public String key() {
    return key;
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
