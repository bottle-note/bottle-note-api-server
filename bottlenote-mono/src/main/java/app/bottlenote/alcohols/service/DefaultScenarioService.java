package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioExceptionCode.*;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioAnswer.Selection;
import app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioQuestionItem;
import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest;
import app.bottlenote.alcohols.dto.request.ScenarioEvaluateRequest.Mode;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import app.bottlenote.alcohols.service.ScenarioEvaluationService.*;
import app.bottlenote.global.pagination.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private final ScenarioOptionCodec optionCodec;
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
    List<Selection> selections = new ArrayList<>();
    for (var answer : r.answers()) {
      selections.add(
          optionCodec.decode(
              answer.optionId(),
              optionCodec.context(id, version, selections),
              answer.questionId()));
    }
    var e =
        evaluator.evaluate(
            p.scenario().getGroupId(),
            p.definition(),
            new EvaluationInput(
                selections.stream().map(Selection::answer).toList(),
                null,
                r.viewQuestionId(),
                0,
                r.size(),
                selections.stream()
                    .filter(Selection::minExclusive)
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
              createOptions(
                  q, e, optionCodec.context(id, version, selections.subList(0, applied))));
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
      List<Selection> applied,
      Evaluation e,
      State state) {
    if (state == State.QUESTION) {
      if (r.cursor() != null) throw ScenarioException.invalid("결과 화면에서만 cursor를 사용합니다.");
      return KeysetPageResponse.of(List.of(), new KeysetPagination(false, null));
    }
    String context =
        "scenario-results-v1:"
            + optionCodec.context(
                published.scenario().getId(), published.version().getId(), applied)
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

  private List<ScenarioOption> createOptions(ScenarioQuestionItem q, Evaluation e, String context) {
    List<Selection> selections = new ArrayList<>();
    if (q.inputKind() == Input.NUMBER_RANGE) {
      numericOptions(q, e, selections);
    } else {
      for (var choice : q.inputConfig().choices())
        selections.add(
            new Selection(
                new ScenarioAnswer(q.id(), AnswerKind.CHOICE, choice.id(), null, null),
                false,
                choice.label()));
    }
    if (q.inputConfig().allowAny())
      selections.add(
          new Selection(
              new ScenarioAnswer(q.id(), AnswerKind.ANY, null, null, null), false, "상관없어요"));
    var counts =
        candidates.countAlternatives(
            e.query(), selections.stream().map(s -> evaluator.withSelection(e, q, s)).toList());
    List<ScenarioOption> options = new ArrayList<>();
    for (int i = 0; i < selections.size(); i++) {
      var s = selections.get(i);
      var a = s.answer();
      long count = counts.get(i);
      if (a.kind() == AnswerKind.NUMBER_RANGE && count == 0) continue;
      options.add(
          new ScenarioOption(
              optionCodec.encode(context, s),
              s.label(),
              a.kind(),
              a.min(),
              a.max(),
              s.minExclusive(),
              count,
              count > 0));
    }
    return List.copyOf(options);
  }

  private void numericOptions(ScenarioQuestionItem q, Evaluation e, List<Selection> selections) {
    var config = q.inputConfig();
    var quartiles = candidates.findQuartiles(e.query(), config.min(), config.max());
    if (quartiles.isEmpty()) return;
    var cuts = new TreeSet<BigDecimal>();
    for (var value : quartiles) {
      BigDecimal cut =
          value
              .subtract(config.min())
              .divide(config.step(), 0, RoundingMode.FLOOR)
              .multiply(config.step())
              .add(config.min());
      if (cut.compareTo(config.max()) < 0) cuts.add(cut);
    }
    cuts.add(config.max());
    BigDecimal lower = config.min();
    boolean exclusive = false;
    for (var upper : cuts) {
      String unit = config.unit() == null ? "" : config.unit();
      String label =
          lower.toPlainString()
              + unit
              + (exclusive ? " 초과 ~ " : " ~ ")
              + upper.toPlainString()
              + unit
              + " 이하";
      selections.add(
          new Selection(
              new ScenarioAnswer(q.id(), AnswerKind.NUMBER_RANGE, null, lower, upper),
              exclusive,
              label));
      lower = upper;
      exclusive = true;
    }
  }

  private record Published(
      AlcoholAttributeScenario scenario,
      AlcoholAttributeScenarioVersion version,
      ScenarioDefinition definition) {}
}
