package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*;
import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.*;
import java.util.List;

public interface AdminAlcoholScenarioService {
  List<ScenarioItem> findAll(Long groupId);

  ScenarioDetail getDetail(Long groupId, Long scenarioId);

  ScenarioDetail create(Long groupId, Create request);

  ScenarioItem update(Long groupId, Long scenarioId, Update request);

  ScenarioVersionDetail createDraft(Long groupId, Long scenarioId, Draft request);

  ScenarioVersionDetail getVersion(Long groupId, Long scenarioId, Long versionId);

  ScenarioVersionDetail save(Long groupId, Long scenarioId, Long versionId, Save request);

  ScenarioDetail publish(Long groupId, Long scenarioId, Long versionId, Publish request);

  ScenarioDeleted delete(Long groupId, Long scenarioId, long expectedLockVersion);

  ScenarioDeleted deleteDraft(
      Long groupId,
      Long scenarioId,
      Long versionId,
      long expectedScenarioLockVersion,
      long expectedLockVersion);

  ScenarioPreview preview(
      Long groupId, Long scenarioId, Long versionId, AdminScenarioPreviewRequest request);
}
