package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity(name = "alcohol_attribute_scenario_stage")
@Table(name = "alcohol_attribute_scenario_stages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeScenarioStage extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long versionId;

  @Column(nullable = false)
  private int position;

  public static AlcoholAttributeScenarioStage create(Long versionId, int position) {
    var s = new AlcoholAttributeScenarioStage();
    s.versionId = versionId;
    s.position = position;
    return s;
  }

  public void moveTo(int position) {
    this.position = position;
  }
}
