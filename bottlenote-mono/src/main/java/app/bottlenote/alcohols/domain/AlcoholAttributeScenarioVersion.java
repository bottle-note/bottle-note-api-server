package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity(name = "alcohol_attribute_scenario_version")
@Table(name = "alcohol_attribute_scenario_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeScenarioVersion extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long scenarioId;

  @Column(nullable = false)
  private int versionNo;

  @Column(nullable = false, columnDefinition = "smallint")
  private int definitionSchemaVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  private Long startQuestionId;

  @Column(nullable = false)
  private long lockVersion;

  private LocalDateTime publishedAt;

  public static AlcoholAttributeScenarioVersion create(Long scenarioId, int versionNo) {
    var v = new AlcoholAttributeScenarioVersion();
    v.scenarioId = scenarioId;
    v.versionNo = versionNo;
    v.definitionSchemaVersion = 1;
    v.status = Status.DRAFT;
    return v;
  }

  public void startAt(Long id) {
    startQuestionId = id;
  }

  public void publish() {
    status = Status.PUBLISHED;
    publishedAt = LocalDateTime.now();
  }
}
