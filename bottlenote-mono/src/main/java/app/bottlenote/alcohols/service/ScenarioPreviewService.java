package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.ScenarioPreview;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioPreviewService {
  private final ScenarioEvaluationService evaluator;
  private final AdminAlcoholAttributeSearchService searchService;

  @Transactional(readOnly = true)
  public ScenarioPreview preview(
      Long groupId, ScenarioDefinition definition, AdminScenarioPreviewRequest request) {
    var e =
        evaluator.evaluate(
            groupId,
            definition,
            new ScenarioEvaluationService.EvaluationInput(
                request.answers(),
                request.changedQuestionId(),
                request.viewQuestionId(),
                request.page(),
                request.size(),
                Set.of()));
    return new ScenarioPreview(
        e.path(),
        e.nextQuestionId(),
        e.completed(),
        e.answers(),
        e.discardedQuestionIds(),
        e.contributions(),
        e.viewedQuestionId(),
        searchService.findCandidates(e.query(), e.attributes(), e.tastingGroups()));
  }
}
