package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_KEY;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 여러 술이 공유하는 속성 사전. 값과 비교 조건은 속성 자체가 소유한다.
 *
 * <p>키는 갱신 기준이고 표시명은 관리자용 이름이다. 술과의 연결은 별도 매핑이 소유한다.
 */
@Getter
@Entity(name = "alcohol_attribute")
@Table(name = "alcohol_attributes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttribute extends BaseEntity {

  private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,63}");
  private static final int VALUE_MAX_LENGTH = 255;
  private static final int DESCRIPTION_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

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

  @Comment("값. datatype에 따라 애플리케이션이 변환")
  @Column(name = "attribute_value", nullable = false, length = VALUE_MAX_LENGTH)
  private String attributeValue;

  @Comment("속성의 의미와 관리자 입력 기준")
  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @Comment("비교 의미. 실제 연결은 관리자가 등록한다")
  @Enumerated(EnumType.STRING)
  @Column(name = "`operator`", nullable = false, length = 10)
  private AttributeOperator operator;

  @Comment("표시 단위. 원, 점 등")
  @Column(name = "unit", length = 20)
  private String unit;

  public static AlcoholAttribute create(
      String attributeKey,
      String displayName,
      AttributeType attributeType,
      AttributeDatatype datatype,
      AttributeOperator operator,
      String attributeValue,
      String unit,
      String description) {
    AlcoholAttribute attribute = new AlcoholAttribute();
    attribute.attributeKey = normalizeKey(attributeKey);
    attribute.update(
        displayName, attributeType, datatype, operator, attributeValue, unit, description);
    return attribute;
  }

  public void update(
      String displayName,
      AttributeType attributeType,
      AttributeDatatype datatype,
      AttributeOperator operator,
      String attributeValue,
      String unit,
      String description) {
    if (attributeType == null
        || datatype == null
        || operator == null
        || (operator == AttributeOperator.GTE && !datatype.isNumeric())) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    String name = normalizeText(displayName, 100, false);
    String value = normalizeValue(datatype, attributeValue);
    String normalizedUnit = normalizeText(unit, 20, true);
    String normalizedDescription = normalizeText(description, DESCRIPTION_MAX_LENGTH, true);
    this.displayName = name;
    this.attributeType = attributeType;
    this.datatype = datatype;
    this.operator = operator;
    this.attributeValue = value;
    this.unit = normalizedUnit;
    this.description = normalizedDescription;
  }

  public void updateFrom(AlcoholAttribute validated) {
    this.displayName = validated.displayName;
    this.attributeType = validated.attributeType;
    this.datatype = validated.datatype;
    this.operator = validated.operator;
    this.attributeValue = validated.attributeValue;
    this.unit = validated.unit;
    this.description = validated.description;
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

  private static String normalizeValue(AttributeDatatype datatype, String attributeValue) {
    if (attributeValue == null) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    String value = attributeValue.trim();
    if (value.length() > VALUE_MAX_LENGTH || !datatype.accepts(value)) {
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    }
    if (datatype.isNumeric()) {
      return new BigDecimal(value).stripTrailingZeros().toPlainString();
    }
    return value;
  }
}
