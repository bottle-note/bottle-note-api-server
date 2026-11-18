package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse;
import app.bottlenote.alcohols.dto.response.ScenarioListItem;
import java.util.List;

public interface ScenarioService {
  List<ScenarioListItem> findAll();

  ScenarioEvaluationResponse get(Long scenarioId);

  ScenarioEvaluationResponse evaluate(Long scenarioId, ScenarioEvaluateRequest request);
}
