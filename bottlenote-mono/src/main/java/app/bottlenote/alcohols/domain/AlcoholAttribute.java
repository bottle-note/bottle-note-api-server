package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_KEY;
import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE;

import app.bottlenote.alcohols.constant.AttributeDatatype;
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 위스키 속성. 키워드·대중성·가격 같은 부가 정보를 출처·기준 시점과 함께 한 행씩 기록하는 원장이다.
 *
 * <p>attributeKey는 자유 문자열이고 type·datatype만 고정 코드다. 값은 문자열로 두고 datatype으로 검증한다.
 */
@Getter
@Entity(name = "alcohol_attribute")
@Table(name = "alcohol_attributes")
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlcoholAttribute extends BaseEntity {

  private static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,63}");
  private static final int VALUE_MAX_LENGTH = 255;
  private static final int DESCRIPTION_MAX_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Comment("주류 ID")
  @Column(name = "alcohol_id", nullable = false)
  private Long alcoholId;

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

  @Comment("근거·출처")
  @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
  private String description;

  @Comment("값의 기준 시점. 가격·점수처럼 시점이 있는 값만")
  @Column(name = "based_at")
  private LocalDateTime basedAt;

  public static AlcoholAttribute create(
      Long alcoholId,
      String attributeKey,
      AttributeType attributeType,
      AttributeDatatype datatype,
      String attributeValue,
      String description,
      LocalDateTime basedAt) {
    Objects.requireNonNull(alcoholId, "alcoholId");
    Objects.requireNonNull(attributeType, "attributeType");
    Objects.requireNonNull(datatype, "datatype");
    String key = normalizeKey(attributeKey);
    String value = normalizeValue(datatype, attributeValue);
    return AlcoholAttribute.builder()
        .alcoholId(alcoholId)
        .attributeKey(key)
        .attributeType(attributeType)
        .datatype(datatype)
        .attributeValue(value)
        .description(normalizeDescription(description))
        .basedAt(basedAt)
        .build();
  }

  /** 항목(key)은 바꾸지 않는다. 분류·형식·값·근거·기준 시점만 고친다. */
  public void update(
      AttributeType attributeType,
      AttributeDatatype datatype,
      String attributeValue,
      String description,
      LocalDateTime basedAt) {
    Objects.requireNonNull(attributeType, "attributeType");
    Objects.requireNonNull(datatype, "datatype");
    this.attributeValue = normalizeValue(datatype, attributeValue);
    this.attributeType = attributeType;
    this.datatype = datatype;
    this.description = normalizeDescription(description);
    this.basedAt = basedAt;
  }

  /** 같은 항목·값인지. 일괄 추가 시 중복을 건너뛰는 기준이다. */
  public boolean hasSameKeyAndValue(String attributeKey, String attributeValue) {
    return this.attributeKey.equals(attributeKey) && this.attributeValue.equals(attributeValue);
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

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    String trimmed = description.trim();
    return trimmed.length() > DESCRIPTION_MAX_LENGTH
        ? trimmed.substring(0, DESCRIPTION_MAX_LENGTH)
        : trimmed;
  }
}
