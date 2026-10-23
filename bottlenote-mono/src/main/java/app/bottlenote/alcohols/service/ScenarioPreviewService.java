package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioException.invalid;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.*;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest.ScenarioAnswer;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest.*;
import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScenarioPreviewService {
  private final AdminAlcoholAttributeSearchService searchService;
  private final ScenarioDefinitionValidator validator;

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public ScenarioPreview preview(
      Long groupId,
      ScenarioDefinitionRequest d,
      Map<Long, AlcoholAttribute> attributes,
      Set<String> codes,
      AdminScenarioPreviewRequest r) {
    var questions =
        d.questions().stream()
            .collect(Collectors.toMap(ScenarioQuestionItem::id, Function.identity()));
    var rules =
        d.transitions().stream()
            .collect(Collectors.groupingBy(ScenarioTransitionItem::fromQuestionId));
    Set<Long> submitted = new HashSet<>();
    for (var a : r.answers()) {
      if (!submitted.add(a.questionId()) || !questions.containsKey(a.questionId()))
        throw invalid("질문 " + a.questionId() + ": 중복 또는 다른 버전의 질문 답변입니다.");
    }
    List<ScenarioAnswer> input = r.answers();
    List<Long> discarded = List.of();
    if (r.changedQuestionId() != null) {
      int index = -1;
      for (int i = 0; i < input.size(); i++)
        if (input.get(i).questionId().equals(r.changedQuestionId())) index = i;
      if (index < 0) throw invalid("changedQuestionId의 답변이 필요합니다.");
      discarded =
          input.subList(index + 1, input.size()).stream().map(ScenarioAnswer::questionId).toList();
      input = input.subList(0, index + 1);
    }
    List<Long> path = new ArrayList<>();
    List<ScenarioAnswer> accepted = new ArrayList<>();
    Long next = d.startQuestionId();
    for (var a : input) {
      if (!Objects.equals(a.questionId(), next))
        throw invalid("질문 " + a.questionId() + ": 현재 유효 경로의 다음 질문 " + next + "에 대한 답변이 필요합니다.");
      var q = questions.get(next);
      validateAnswer(q, a);
      path.add(next);
      accepted.add(a);
      var rule =
          rules.get(next).stream()
              .sorted(Comparator.comparing(ScenarioTransitionItem::priority))
              .filter(t -> matches(t, a))
              .findFirst()
              .orElseThrow(() -> invalid("질문 " + a.questionId() + ": 일치하는 명시적 이동 규칙이 없습니다."));
      next = rule.targetKind() == Target.END ? null : rule.targetQuestionId();
    }
    if (next != null) path.add(next);
    int prefix = accepted.size();
    Long viewed = r.viewQuestionId() == null ? next : r.viewQuestionId();
    if (r.viewQuestionId() != null) {
      prefix = path.indexOf(r.viewQuestionId());
      if (prefix < 0) throw invalid("viewQuestionId는 현재 유효 경로의 질문이어야 합니다.");
    }
    List<ScenarioContribution> contributions = new ArrayList<>();
    List<AdminAlcoholCandidateRequest> filters = new ArrayList<>();
    var conditions = new PreviewConditions(contributions, filters);
    for (var a : accepted.subList(0, prefix))
      contribute(questions.get(a.questionId()), a, attributes, codes, conditions);
    NextQuestion facet = null;
    if (viewed != null) {
      var q = questions.get(viewed);
      facet =
          new NextQuestion(
              AdminAlcoholCandidateRequest.Source.valueOf(q.sourceKind().name()),
              q.attributeId(),
              List.of());
    }
    var candidates =
        searchService.findScenarioCandidates(groupId, filters, facet, r.page(), r.size());
    return new ScenarioPreview(
        List.copyOf(path),
        next,
        next == null,
        List.copyOf(accepted),
        discarded,
        List.copyOf(contributions),
        viewed,
        candidates);
  }

  private void validateAnswer(ScenarioQuestionItem q, ScenarioAnswer a) {
    String at = "질문 " + q.id() + ": ";
    if (a.kind() == null) throw invalid(at + "답변 kind가 필요합니다.");
    var c = q.inputConfig();
    switch (a.kind()) {
      case ANY -> {
        if (!c.allowAny() || a.choiceId() != null || a.min() != null || a.max() != null)
          throw invalid(at + "허용된 ANY는 다른 답변 값을 포함하지 않습니다.");
      }
      case CHOICE -> {
        if (q.inputKind() != Input.SINGLE_CHOICE
            || a.min() != null
            || a.max() != null
            || c.choices().stream().noneMatch(x -> x.id().equals(a.choiceId())))
          throw invalid(at + "존재하는 choiceId 하나가 필요합니다.");
      }
      case NUMBER_RANGE -> {
        if (q.inputKind() != Input.NUMBER_RANGE
            || a.choiceId() != null
            || a.min() == null
            || a.max() == null
            || a.min().precision() > 21
            || a.max().precision() > 21
            || a.min().scale() > 6
            || a.max().scale() > 6
            || a.min().compareTo(a.max()) > 0
            || a.min().compareTo(c.min()) < 0
            || a.max().compareTo(c.max()) > 0
            || a.min().subtract(c.min()).remainder(c.step()).signum() != 0
            || a.max().subtract(c.min()).remainder(c.step()).signum() != 0)
          throw invalid(at + "설정한 min/max/step에 맞는 숫자 범위가 필요합니다.");
      }
    }
  }

  private boolean matches(ScenarioTransitionItem t, ScenarioAnswer a) {
    return t.predicate().matches(a.kind(), a.choiceId(), a.min(), a.max());
  }

  private record PreviewConditions(
      List<ScenarioContribution> contributions, List<AdminAlcoholCandidateRequest> filters) {}

  private void contribute(
      ScenarioQuestionItem q,
      ScenarioAnswer a,
      Map<Long, AlcoholAttribute> attrs,
      Set<String> codes,
      PreviewConditions conditions) {
    if (a.kind() == AnswerKind.ANY) return;
    var out = conditions.contributions();
    var filters = conditions.filters();
    List<Condition> include = new ArrayList<>();
    List<AlcoholCategoryGroup> categories = List.of();
    List<String> tasting = List.of();
    BigDecimal from = null, to = null;
    if (a.kind() == AnswerKind.CHOICE) {
      var choice =
          q.inputConfig().choices().stream()
              .filter(c -> c.id().equals(a.choiceId()))
              .findFirst()
              .orElseThrow();
      var value =
          validator.choiceValue(
              q,
              q.attributeId() == null ? null : attrs.get(q.attributeId()),
              codes,
              choice.value());
      out.add(new ScenarioContribution(q.id(), q.sourceKind(), q.attributeId(), "EQ", value));
      switch (q.sourceKind()) {
        case ATTRIBUTE -> {
          if (attrs.get(q.attributeId()).getDatatype().isNumeric()) {
            include.add(new Condition(q.attributeId(), AttributeSearchOperator.GTE, value));
            include.add(new Condition(q.attributeId(), AttributeSearchOperator.LTE, value));
          } else include.add(new Condition(q.attributeId(), AttributeSearchOperator.EQ, value));
        }
        case CATEGORY -> categories = List.of(AlcoholCategoryGroup.valueOf(value));
        case TASTING_GROUP -> tasting = List.of(value);
        case ABV -> {
          from = new BigDecimal(value);
          to = from;
        }
      }
    } else {
      out.add(
          new ScenarioContribution(
              q.id(), q.sourceKind(), q.attributeId(), "GTE", a.min().toPlainString()));
      out.add(
          new ScenarioContribution(
              q.id(), q.sourceKind(), q.attributeId(), "LTE", a.max().toPlainString()));
      if (q.sourceKind() == ScenarioTypes.Source.ATTRIBUTE) {
        include.add(
            new Condition(
                q.attributeId(),
                AttributeSearchOperator.GTE,
                a.min().stripTrailingZeros().toPlainString()));
        include.add(
            new Condition(
                q.attributeId(),
                AttributeSearchOperator.LTE,
                a.max().stripTrailingZeros().toPlainString()));
      } else {
        from = a.min();
        to = a.max();
      }
    }
    filters.add(
        new AdminAlcoholCandidateRequest(
            null, include, List.of(), categories, from, to, tasting, null, 0, 25));
  }
}
