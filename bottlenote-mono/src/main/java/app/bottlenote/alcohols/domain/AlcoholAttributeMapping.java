package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  public static AlcoholAttributeMapping create(Long alcoholId, Long attributeId) {
    AlcoholAttributeMapping mapping = new AlcoholAttributeMapping();
    mapping.alcoholId = Objects.requireNonNull(alcoholId, "alcoholId");
    mapping.attributeId = Objects.requireNonNull(attributeId, "attributeId");
    return mapping;
  }
}
