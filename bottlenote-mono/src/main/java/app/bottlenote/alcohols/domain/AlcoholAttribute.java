package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_KEY;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_RULE;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Type;

/**
 * 여러 술이 공유하는 속성 정의. 실제 값은 술별 연결이 소유한다.
 *
 * <p>키는 갱신 기준이고 표시명은 관리자용 이름이다. 술과의 연결은 별도 매핑이 소유한다.
 */
@Getter
@Entity(name = "alcohol_attribute")
@Table(name = "alcohol_attributes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttribute extends BaseEntity {

  private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,63}");
  private static final int DESCRIPTION_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "group_id")
  private Long groupId;

  public void changeGroup(Long groupId) {
    if (groupId != null && groupId <= 0)
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    this.groupId = groupId;
  }

  @Comment("관리자가 보는 고유 속성 이름")
  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Comment("속성 항목(key). 영문 소문자·숫자·밑줄 자유 입력")
  @Column(name = "attribute_key", nullable = false, length = 64)
  private String attributeKey;

  @Comment("분류 코드")
  @Enumerated(EnumType.STRING)
  @Column(name = "attribute_type", nullable = false, length = 20)
  private AttributeType attributeType;

  @Comment("값 형식 코드")
  @Enumerated(EnumType.STRING)
  @Column(name = "datatype", nullable = false, length = 10)
  private AttributeDatatype datatype;

  @Column(name = "allow_multiple_values", nullable = false)
  private boolean allowMultipleValues;

  public void changeAllowMultipleValues(boolean allowMultipleValues) {
    this.allowMultipleValues = allowMultipleValues;
  }

  @Comment("속성의 의미와 관리자 입력 기준")
  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @Comment("표시 단위. 원, 점 등")
  @Column(name = "unit", length = 20)
  private String unit;

  @Type(JsonType.class)
  @Column(name = "allowed_values", columnDefinition = "json")
  private List<String> allowedValues;

  @Column(name = "value_pattern", length = 500)
  private String pattern;

  public List<String> getAllowedValues() {
    return allowedValues == null ? null : List.copyOf(allowedValues);
  }

  public void changeStringRules(List<String> allowedValues, String pattern) {
    if (datatype != AttributeDatatype.STRING && (allowedValues != null || pattern != null))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
    Pattern compiled = compileRule(pattern);
    List<String> values = null;
    if (allowedValues != null) {
      if (allowedValues.isEmpty() || allowedValues.size() > 100)
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
      values =
          allowedValues.stream()
              .map(
                  value -> {
                    if (value == null || value.isBlank() || value.trim().length() > 255)
                      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
                    String normalized = value.trim();
                    if (compiled != null && !compiled.matcher(normalized).matches())
                      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
                    return normalized;
                  })
              .toList();
      if (new HashSet<>(values).size() != values.size())
        throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
    }
    this.allowedValues = values;
    this.pattern = pattern;
  }

  public void validateValue(String value) {
    if ((allowedValues != null && !allowedValues.contains(value))
        || (pattern != null && !Pattern.matches(pattern, value)))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
  }

  private static Pattern compileRule(String pattern) {
    if (pattern == null) return null;
    if (pattern.isBlank() || pattern.length() > 500)
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
    try {
      return Pattern.compile(pattern);
    } catch (PatternSyntaxException e) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
    }
  }

  public static AlcoholAttribute create(
      String attributeKey,
      String displayName,
      AttributeType attributeType,
      AttributeDatatype datatype,
      String unit,
      String description) {
    AlcoholAttribute attribute = new AlcoholAttribute();
    attribute.attributeKey = normalizeKey(attributeKey);
    attribute.update(displayName, attributeType, datatype, unit, description);
    return attribute;
  }

  public void update(
      String displayName,
      AttributeType attributeType,
      AttributeDatatype datatype,
      String unit,
      String description) {
    if (attributeType == null || datatype == null) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    String name = normalizeText(displayName, 100, false);
    String normalizedUnit = normalizeText(unit, 20, true);
    String normalizedDescription = normalizeText(description, DESCRIPTION_MAX_LENGTH, true);
    if (datatype != AttributeDatatype.STRING && (allowedValues != null || pattern != null))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_RULE);
    this.displayName = name;
    this.attributeType = attributeType;
    this.datatype = datatype;
    this.unit = normalizedUnit;
    this.description = normalizedDescription;
  }

  public void updateFrom(AlcoholAttribute validated) {
    this.displayName = validated.displayName;
    this.attributeType = validated.attributeType;
    this.datatype = validated.datatype;
    this.allowMultipleValues = validated.allowMultipleValues;
    this.unit = validated.unit;
    this.description = validated.description;
    this.allowedValues = validated.getAllowedValues();
    this.pattern = validated.pattern;
  }

  private static String normalizeText(String text, int maxLength, boolean optional) {
    if (text == null || text.isBlank()) {
      if (optional) return null;
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    String value = text.trim();
    if (value.length() > maxLength) throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    return value;
  }

  private static String normalizeKey(String attributeKey) {
    if (attributeKey == null) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_KEY);
    }
    String key = attributeKey.trim();
    if (!KEY_PATTERN.matcher(key).matches()) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_KEY);
    }
    return key;
  }

  public String normalizeSearchValue(AttributeSearchOperator operator, String value) {
    if (!operator.requiresValue()) {
      if (value != null) throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
      return null;
    }
    if (value == null
        || (operator.isNumericComparison() && !datatype.isNumeric())
        || (operator == AttributeSearchOperator.CONTAINS && datatype != AttributeDatatype.STRING))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    return AlcoholAttributeMapping.normalizeValue(datatype, value);
  }
}
