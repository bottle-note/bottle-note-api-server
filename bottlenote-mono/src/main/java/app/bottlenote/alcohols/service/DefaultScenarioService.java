package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;

import app.bottlenote.alcohols.constant.ScenarioTypes.Status;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest;
import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest.Mode;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.alcohols.service.ScenarioEvaluationService.*;
import app.bottlenote.global.pagination.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultScenarioService implements ScenarioService {
  private final AlcoholScenarioRepository scenarios;
  private final AlcoholScenarioVersionRepository versions;
  private final ScenarioDefinitionStore definitions;
  private final ScenarioEvaluationService evaluator;
  private final AlcoholCandidateRepository candidates;
  private final ScenarioOptionCodec options;
  private final ScenarioOptionService optionService;
  private final HmacCursorCodec cursors;

  @Override
  @Transactional(readOnly = true)
  public ScenarioEvaluationResponse get(Long scenarioId) {
    var published = published(scenarioId);
    return evaluate(
        published,
        new ScenarioEvaluateRequest(
            published.version().getId(), List.of(), Mode.QUESTION, null, null, 20));
  }

  @Override
  @Transactional(readOnly = true)
  public ScenarioEvaluationResponse evaluate(Long scenarioId, ScenarioEvaluateRequest request) {
    var published = published(scenarioId);
    if (!published.version().getId().equals(request.versionId()))
      throw new ScenarioException(SCENARIO_CONFLICT);
    return evaluate(published, request);
  }

  private Published published(Long id) {
    var s = scenarios.findById(id).orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
    if (!s.isEnabled() || s.getActiveVersionId() == null)
      throw new ScenarioException(SCENARIO_NOT_FOUND);
    var v =
        versions
            .findById(s.getActiveVersionId())
            .orElseThrow(() -> new ScenarioException(SCENARIO_NOT_FOUND));
    if (v.getStatus() != Status.PUBLISHED || !v.getScenarioId().equals(s.getId()))
      throw new ScenarioException(SCENARIO_NOT_FOUND);
    return new Published(s, v, definitions.read(v));
  }

  private ScenarioEvaluationResponse evaluate(Published p, ScenarioEvaluateRequest r) {
    Long id = p.scenario().getId(), version = p.version().getId();
    List<ScenarioSelection> selections = new ArrayList<>();
    for (var answer : r.answers()) {
      selections.add(
          options.decode(
              answer.optionId(), options.context(id, version, selections), answer.questionId()));
    }
    var e =
        evaluator.evaluate(
            p.scenario().getGroupId(),
            p.definition(),
            new EvaluationInput(
                selections.stream().map(ScenarioSelection::answer).toList(),
                null,
                r.viewQuestionId(),
                0,
                r.size(),
                selections.stream()
                    .filter(ScenarioSelection::minExclusive)
                    .map(s -> s.answer().questionId())
                    .collect(Collectors.toSet())));
    int applied =
        r.viewQuestionId() == null ? selections.size() : e.path().indexOf(r.viewQuestionId());
    State state =
        r.mode() == Mode.RESULT
            ? State.USER_FINISHED
            : e.viewedQuestionId() == null ? State.SCENARIO_END : State.QUESTION;
    ScenarioQuestion question = null;
    long total = candidates.count(e.query());
    if (state == State.QUESTION) {
      var q =
          p.definition().questions().stream()
              .filter(item -> item.id().equals(e.viewedQuestionId()))
              .findFirst()
              .orElseThrow();
      question =
          new ScenarioQuestion(
              q.id(),
              q.title(),
              optionService.options(
                  q, e, options.context(id, version, selections.subList(0, applied))));
    }
    List<ScenarioAnswerView> answers = new ArrayList<>();
    for (int i = 0; i < selections.size(); i++) {
      var submitted = r.answers().get(i);
      answers.add(
          new ScenarioAnswerView(
              submitted.questionId(),
              submitted.optionId(),
              selections.get(i).label(),
              i < applied));
    }
    return new ScenarioEvaluationResponse(
        id,
        p.scenario().getTitle(),
        version,
        state,
        e.nextQuestionId(),
        e.path(),
        List.copyOf(answers),
        question,
        total,
        resultPage(p, r, selections.subList(0, applied), e, state));
  }

  private KeysetPageResponse<List<ScenarioCandidate>> resultPage(
      Published published,
      ScenarioEvaluateRequest r,
      List<ScenarioSelection> applied,
      Evaluation e,
      State state) {
    if (state == State.QUESTION) {
      if (r.cursor() != null) throw ScenarioException.invalid("결과 화면에서만 cursor를 사용합니다.");
      return KeysetPageResponse.of(List.of(), new KeysetPagination(false, null));
    }
    String context =
        "scenario-results-v1:"
            + options.context(published.scenario().getId(), published.version().getId(), applied)
            + ":"
            + r.size();
    Long after =
        r.cursor() == null
            ? null
            : CursorKeys.requireLong(cursors.verify(r.cursor(), context), "id");
    var slice =
        KeysetPagination.fromOverflow(
            candidates.findAfter(e.query(), after, r.size() + 1),
            r.size(),
            alcohol -> cursors.encode(context, Map.of("id", alcohol.getId().toString())));
    return KeysetPageResponse.of(
        slice.items().stream()
            .map(
                a ->
                    new ScenarioCandidate(
                        a.getId(),
                        a.getKorName(),
                        a.getEngName(),
                        a.getImageUrl(),
                        a.getKorCategory(),
                        a.getAbv()))
            .toList(),
        slice.pagination());
  }

  private record Published(
      AlcoholAttributeScenario scenario,
      AlcoholAttributeScenarioVersion version,
      ScenarioDefinition definition) {}
}
