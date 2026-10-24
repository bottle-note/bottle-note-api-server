package app.bottlenote.alcohols.controller;

import static app.bottlenote.global.annotation.SecurityPolicy.AuthType.OPTIONAL_AUTH;

import app.bottlenote.alcohols.controller.docs.ScenarioApiDocs;
import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest;
import app.bottlenote.alcohols.service.ScenarioService;
import app.bottlenote.global.annotation.SecurityPolicy;
import app.bottlenote.global.data.response.GlobalResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scenarios")
@RequiredArgsConstructor
@Validated
@SecurityPolicy(auth = OPTIONAL_AUTH)
@ScenarioApiDocs.ApiTag
public class ScenarioController {
  private final ScenarioService service;

  @GetMapping("/{scenarioId}")
  @ScenarioApiDocs.Get
  public ResponseEntity<GlobalResponse> get(@PathVariable @Positive Long scenarioId) {
    return GlobalResponse.ok(service.get(scenarioId));
  }

  @PostMapping("/{scenarioId}/evaluate")
  @ScenarioApiDocs.Evaluate
  public ResponseEntity<GlobalResponse> getEvaluation(
      @PathVariable @Positive Long scenarioId,
      @RequestBody @Valid ScenarioEvaluateRequest request) {
    return GlobalResponse.ok(service.evaluate(scenarioId, request));
  }
}
