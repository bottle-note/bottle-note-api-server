package app.bottlenote.alcohols.domain;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE;

import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity(name = "alcohol_attribute_group")
@Table(name = "alcohol_attribute_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeGroup extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private int displayOrder;

  public static AlcoholAttributeGroup create(
      String name, String description, Integer displayOrder) {
    var group = new AlcoholAttributeGroup();
    group.update(name, description, displayOrder);
    return group;
  }

  public void update(String name, String description, Integer displayOrder) {
    if (name == null
        || name.isBlank()
        || name.trim().length() > 100
        || (description != null && description.trim().length() > 500)
        || (displayOrder != null && displayOrder < 0))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE);
    this.name = name.trim();
    this.description = description == null || description.isBlank() ? null : description.trim();
    this.displayOrder = displayOrder == null ? 0 : displayOrder;
  }
}
