package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.common.domain.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@Getter
@Entity(name = "alcohol_attribute_scenario_transition")
@Table(name = "alcohol_attribute_scenario_transitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeScenarioTransition extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long versionId;

  @Column(nullable = false)
  private Long fromQuestionId;

  @Column(nullable = false)
  private int priority;

  @Type(JsonType.class)
  @Column(nullable = false, columnDefinition = "json")
  private ScenarioPredicate predicate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Target targetKind;

  private Long targetQuestionId;

  public static AlcoholAttributeScenarioTransition create(Long versionId) {
    var t = new AlcoholAttributeScenarioTransition();
    t.versionId = versionId;
    return t;
  }

  public void configure(
      Long from, int priority, ScenarioPredicate predicate, Target target, Long to) {
    this.fromQuestionId = from;
    this.priority = priority;
    this.predicate = predicate;
    this.targetKind = target;
    this.targetQuestionId = to;
  }

  public void moveTo(int priority) {
    this.priority = priority;
  }
}
