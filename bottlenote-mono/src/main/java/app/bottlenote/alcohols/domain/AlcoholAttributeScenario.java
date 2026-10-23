package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity(name = "alcohol_attribute_scenario")
@Table(name = "alcohol_attribute_scenarios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholAttributeScenario extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long groupId;

  @Column(nullable = false, columnDefinition = "tinyint")
  private int slot;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(name = "is_enabled", nullable = false)
  private boolean enabled;

  private Long activeVersionId;

  @Column(nullable = false)
  private long lockVersion;

  public static AlcoholAttributeScenario create(Long groupId, int slot, String title) {
    if (groupId == null || groupId <= 0 || slot < 1 || slot > 3)
      throw app.bottlenote.alcohols.exception.ScenarioException.invalid(
          "slot은 1~3이며 올바른 그룹 ID가 필요합니다.");
    requireTitle(title);
    var s = new AlcoholAttributeScenario();
    s.groupId = groupId;
    s.slot = slot;
    s.title = title.trim();
    s.enabled = true;
    return s;
  }

  public void update(String title, boolean enabled) {
    requireTitle(title);
    this.title = title.trim();
    this.enabled = enabled;
  }

  public void activate(Long versionId) {
    this.activeVersionId = versionId;
  }

  private static void requireTitle(String title) {
    if (title == null || title.isBlank() || title.trim().length() > 100)
      throw app.bottlenote.alcohols.exception.ScenarioException.invalid("시나리오 이름은 1~100자여야 합니다.");
  }
}
