package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse;

public interface ScenarioService {
  ScenarioEvaluationResponse get(Long scenarioId);

  ScenarioEvaluationResponse evaluate(Long scenarioId, ScenarioEvaluateRequest request);
}
