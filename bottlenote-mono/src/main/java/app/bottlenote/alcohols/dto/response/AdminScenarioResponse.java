package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest.ScenarioAnswer;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminScenarioResponse {
  private AdminScenarioResponse() {}

  public record ScenarioItem(
      Long id,
      Long groupId,
      int slot,
      String title,
      boolean enabled,
      Long activeVersionId,
      long lockVersion,
      LocalDateTime createAt,
      LocalDateTime lastModifyAt,
      AdminAuditPrincipalItem createPrincipal,
      AdminAuditPrincipalItem lastModifyPrincipal) {}

  public record ScenarioDetail(ScenarioItem scenario, List<ScenarioVersionItem> versions) {}

  public record ScenarioVersionItem(
      Long id,
      int versionNo,
      Status status,
      long lockVersion,
      LocalDateTime publishedAt,
      LocalDateTime createAt,
      LocalDateTime lastModifyAt,
      AdminAuditPrincipalItem createPrincipal,
      AdminAuditPrincipalItem lastModifyPrincipal) {}

  public record ScenarioVersionDetail(
      ScenarioVersionItem version, ScenarioDefinitionRequest definition) {}

  public record ScenarioDeleted(Long id) {}

  public record ScenarioContribution(
      Long questionId, Source source, Long attributeId, String operator, String value) {}

  public record ScenarioPreview(
      List<Long> path,
      Long nextQuestionId,
      boolean completed,
      List<ScenarioAnswer> answers,
      List<Long> discardedQuestionIds,
      List<ScenarioContribution> contributions,
      Long viewedQuestionId,
      AdminAlcoholCandidateResponse candidates) {}
}
