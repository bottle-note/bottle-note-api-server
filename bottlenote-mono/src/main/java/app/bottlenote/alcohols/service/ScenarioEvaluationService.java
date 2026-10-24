package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioException.invalid;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.domain.AlcoholCandidateRepository;
import app.bottlenote.alcohols.domain.ScenarioAnswer;
import app.bottlenote.alcohols.domain.ScenarioAnswer.ScenarioContribution;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import app.bottlenote.alcohols.domain.ScenarioSelection;
import app.bottlenote.alcohols.domain.TastingTagGroup;
import app.bottlenote.alcohols.domain.TastingTagGroupRepository;
import app.bottlenote.alcohols.domain.TastingTagGroupTag;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria;
import app.bottlenote.alcohols.dto.dsl.AlcoholCandidateCriteria.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioEvaluationService {
  private final ScenarioDefinitionValidator validator;
  private final AlcoholCandidateRepository candidateRepository;
  private final TastingTagGroupRepository tastingGroupRepository;

  @Transactional(readOnly = true)
  public Evaluation evaluate(Long groupId, ScenarioDefinition d, EvaluationInput r) {
    var attributes =
        candidateRepository.findAttributes(groupId).stream()
            .collect(Collectors.toMap(AlcoholAttribute::getId, Function.identity()));
    var groups = tastingGroupRepository.findAllWithTags();
    var codes = groups.stream().map(TastingTagGroup::getCode).collect(Collectors.toSet());
    validator.validate(d, groupId, attributes, codes, true);
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
    for (var a : accepted.subList(0, prefix))
      contributions.addAll(
          contribute(
              questions.get(a.questionId()),
              a,
              attributes,
              codes,
              r.exclusiveMinQuestionIds().contains(a.questionId())));
    NextQuestionCriteria facet = null;
    if (viewed != null) {
      var q = questions.get(viewed);
      facet = new NextQuestionCriteria(q.sourceKind(), q.attributeId(), List.of());
    }
    var query =
        candidateQuery(
            new AlcoholCandidateCriteria(
                groupId, null, List.of(), List.of(), List.of(), facet, r.page(), r.size()),
            contributions,
            attributes,
            groups);
    return new Evaluation(
        List.copyOf(path),
        next,
        next == null,
        List.copyOf(accepted),
        discarded,
        List.copyOf(contributions),
        viewed,
        query,
        attributes,
        groups);
  }

  AlcoholCandidateCriteria withSelection(
      Evaluation e, ScenarioQuestionItem question, ScenarioSelection selection) {
    List<ScenarioContribution> contributions = new ArrayList<>(e.contributions());
    var codes =
        e.tastingGroups().stream().map(TastingTagGroup::getCode).collect(Collectors.toSet());
    contributions.addAll(
        contribute(question, selection.answer(), e.attributes(), codes, selection.minExclusive()));
    return candidateQuery(e.query(), contributions, e.attributes(), e.tastingGroups());
  }

  private AlcoholCandidateCriteria candidateQuery(
      AlcoholCandidateCriteria base,
      List<ScenarioContribution> contributions,
      Map<Long, AlcoholAttribute> attributes,
      List<TastingTagGroup> groups) {
    List<ConditionCriteria> include = new ArrayList<>();
    List<AlcoholCandidateCriteria.BaseFilterCriteria> filters = new ArrayList<>();
    for (var c : contributions) {
      var operator = AttributeSearchOperator.valueOf(c.operator());
      switch (c.source()) {
        case ATTRIBUTE -> {
          var attribute = attributes.get(c.attributeId());
          if (operator == AttributeSearchOperator.EQ && attribute.getDatatype().isNumeric()) {
            include.add(
                new ConditionCriteria(c.attributeId(), AttributeSearchOperator.GTE, c.value()));
            include.add(
                new ConditionCriteria(c.attributeId(), AttributeSearchOperator.LTE, c.value()));
          } else include.add(new ConditionCriteria(c.attributeId(), operator, c.value()));
        }
        case CATEGORY ->
            filters.add(
                new AlcoholCandidateCriteria.BaseFilterCriteria(
                    List.of(AlcoholCategoryGroup.valueOf(c.value())),
                    null,
                    null,
                    List.of(),
                    false));
        case TASTING_GROUP ->
            filters.add(
                new AlcoholCandidateCriteria.BaseFilterCriteria(
                    List.of(),
                    null,
                    null,
                    groups.stream()
                        .filter(g -> g.getCode().equals(c.value()))
                        .flatMap(g -> g.getTags().stream())
                        .filter(TastingTagGroupTag::isCore)
                        .map(TastingTagGroupTag::getTastingTagId)
                        .distinct()
                        .toList(),
                    true));
        case ABV -> {
          var value = new BigDecimal(c.value());
          filters.add(
              new AlcoholCandidateCriteria.BaseFilterCriteria(
                  List.of(),
                  operator == AttributeSearchOperator.LTE ? null : value,
                  operator == AttributeSearchOperator.GTE || operator == AttributeSearchOperator.GT
                      ? null
                      : value,
                  List.of(),
                  false,
                  operator == AttributeSearchOperator.GT));
        }
      }
    }
    return new AlcoholCandidateCriteria(
        base.groupId(),
        null,
        include,
        List.of(),
        filters,
        base.nextQuestion(),
        base.page(),
        base.size());
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

  private List<ScenarioContribution> contribute(
      ScenarioQuestionItem q,
      ScenarioAnswer a,
      Map<Long, AlcoholAttribute> attrs,
      Set<String> codes,
      boolean minExclusive) {
    if (a.kind() == AnswerKind.ANY) return List.of();
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
      return List.of(
          new ScenarioContribution(q.id(), q.sourceKind(), q.attributeId(), "EQ", value));
    } else {
      return List.of(
          new ScenarioContribution(
              q.id(),
              q.sourceKind(),
              q.attributeId(),
              minExclusive ? "GT" : "GTE",
              a.min().toPlainString()),
          new ScenarioContribution(
              q.id(), q.sourceKind(), q.attributeId(), "LTE", a.max().toPlainString()));
    }
  }

  public record EvaluationInput(
      List<ScenarioAnswer> answers,
      Long changedQuestionId,
      Long viewQuestionId,
      int page,
      int size,
      Set<Long> exclusiveMinQuestionIds) {}

  public record Evaluation(
      List<Long> path,
      Long nextQuestionId,
      boolean completed,
      List<ScenarioAnswer> answers,
      List<Long> discardedQuestionIds,
      List<ScenarioContribution> contributions,
      Long viewedQuestionId,
      AlcoholCandidateCriteria query,
      Map<Long, AlcoholAttribute> attributes,
      List<TastingTagGroup> tastingGroups) {}
}
