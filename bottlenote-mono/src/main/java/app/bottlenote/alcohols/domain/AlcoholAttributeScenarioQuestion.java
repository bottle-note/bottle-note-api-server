package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.common.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@Getter
@Entity(name = "alcohol_attribute_scenario_question")
@Table(name = "alcohol_attribute_scenario_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeScenarioQuestion extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long versionId;

  @Column(nullable = false)
  private Long stageId;

  @Column(nullable = false)
  private int branchOrder;

  @Column(nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Source sourceKind;

  private Long attributeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Input inputKind;

  @Type(JsonType.class)
  @Column(nullable = false, columnDefinition = "json")
  private ScenarioInputConfig inputConfig;

  public static AlcoholAttributeScenarioQuestion create(Long versionId) {
    var q = new AlcoholAttributeScenarioQuestion();
    q.versionId = versionId;
    return q;
  }

  public void configure(
      Long stageId,
      int order,
      String title,
      Source source,
      Long attributeId,
      Input input,
      ScenarioInputConfig config) {
    this.stageId = stageId;
    this.branchOrder = order;
    this.title = title.trim();
    this.sourceKind = source;
    this.attributeId = attributeId;
    this.inputKind = input;
    this.inputConfig = config;
  }

  public void moveTo(int order) {
    this.branchOrder = order;
  }
}
