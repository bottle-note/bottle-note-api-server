package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioQuestionItem;
import app.bottlenote.alcohols.dto.response.ScenarioEvaluationResponse.ScenarioOption;
import app.bottlenote.alcohols.service.ScenarioEvaluationService.Evaluation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioOptionService {
  private final AlcoholCandidateRepository repository;
  private final ScenarioOptionCodec codec;
  private final ScenarioEvaluationService evaluator;

  @Transactional(readOnly = true)
  public List<ScenarioOption> options(ScenarioQuestionItem q, Evaluation e, String context) {
    List<ScenarioSelection> selections = new ArrayList<>();
    if (q.inputKind() == Input.NUMBER_RANGE) {
      numericOptions(q, e, selections);
    } else {
      for (var choice : q.inputConfig().choices())
        selections.add(
            new ScenarioSelection(
                new ScenarioAnswer(q.id(), AnswerKind.CHOICE, choice.id(), null, null),
                false,
                choice.label()));
    }
    if (q.inputConfig().allowAny())
      selections.add(
          new ScenarioSelection(
              new ScenarioAnswer(q.id(), AnswerKind.ANY, null, null, null), false, "상관없어요"));
    var counts =
        repository.countAlternatives(
            e.query(), selections.stream().map(s -> evaluator.withSelection(e, q, s)).toList());
    List<ScenarioOption> options = new ArrayList<>();
    for (int i = 0; i < selections.size(); i++) {
      var s = selections.get(i);
      var a = s.answer();
      long count = counts.get(i);
      if (a.kind() == AnswerKind.NUMBER_RANGE && count == 0) continue;
      options.add(
          new ScenarioOption(
              codec.encode(context, s),
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

  private void numericOptions(
      ScenarioQuestionItem q, Evaluation e, List<ScenarioSelection> selections) {
    var config = q.inputConfig();
    var quartiles = repository.findQuartiles(e.query(), config.min(), config.max());
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
          new ScenarioSelection(
              new ScenarioAnswer(q.id(), AnswerKind.NUMBER_RANGE, null, lower, upper),
              exclusive,
              label));
      lower = upper;
      exclusive = true;
    }
  }
}
