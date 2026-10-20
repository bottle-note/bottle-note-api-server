package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity(name = "alcohol_attribute_mapping")
@Table(name = "alcohol_attribute_mappings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeMapping extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "alcohol_id", nullable = false)
  private Long alcoholId;

  @Column(name = "attribute_id", nullable = false)
  private Long attributeId;

  @Column(name = "attribute_value", nullable = false, length = 255)
  private String attributeValue;

  public static AlcoholAttributeMapping create(
      Long alcoholId, AlcoholAttribute attribute, String value) {
    AlcoholAttributeMapping mapping = new AlcoholAttributeMapping();
    mapping.alcoholId = Objects.requireNonNull(alcoholId, "alcoholId");
    mapping.attributeId = Objects.requireNonNull(attribute.getId(), "attributeId");
    mapping.updateValue(attribute, value);
    return mapping;
  }

  public void updateValue(AlcoholAttribute attribute, String value) {
    String normalized = normalizeValue(attribute.getDatatype(), value);
    attribute.validateValue(normalized);
    this.attributeValue = normalized;
  }

  public static String normalizeValue(AttributeDatatype datatype, String raw) {
    if (datatype == null || raw == null)
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    String value = raw.trim();
    if (value.length() > 255 || !datatype.accepts(value))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_INVALID_VALUE);
    return datatype.isNumeric()
        ? new BigDecimal(value).stripTrailingZeros().toPlainString()
        : value;
  }
}
