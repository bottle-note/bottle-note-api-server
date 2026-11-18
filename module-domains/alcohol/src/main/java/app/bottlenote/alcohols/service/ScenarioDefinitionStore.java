package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScenarioDefinitionStore {
  private final AlcoholScenarioRepository repository;
  private final AlcoholScenarioStageRepository stages;
  private final AlcoholScenarioQuestionRepository questions;
  private final AlcoholScenarioTransitionRepository transitions;

  public ScenarioDefinition read(AlcoholAttributeScenarioVersion v) {
    var stageRows = stages.findStages(v.getId());
    Map<Long, Integer> positions = new HashMap<>();
    stageRows.forEach(s -> positions.put(s.getId(), s.getPosition()));
    return new ScenarioDefinition(
        v.getDefinitionSchemaVersion(),
        v.getStartQuestionId(),
        stageRows.stream().map(s -> new ScenarioStageItem(s.getId(), s.getPosition())).toList(),
        questions.findQuestions(v.getId()).stream()
            .sorted(
                Comparator.comparing(
                        (AlcoholAttributeScenarioQuestion q) -> positions.get(q.getStageId()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(AlcoholAttributeScenarioQuestion::getBranchOrder))
            .map(
                q ->
                    new ScenarioQuestionItem(
                        q.getId(),
                        q.getStageId(),
                        q.getBranchOrder(),
                        q.getTitle(),
                        q.getSourceKind(),
                        q.getAttributeId(),
                        q.getInputKind(),
                        q.getInputConfig()))
            .toList(),
        transitions.findTransitions(v.getId()).stream()
            .map(
                t ->
                    new ScenarioTransitionItem(
                        t.getId(),
                        t.getFromQuestionId(),
                        t.getPriority(),
                        t.getPredicate(),
                        t.getTargetKind(),
                        t.getTargetQuestionId()))
            .toList());
  }

  private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
    return list.stream().collect(Collectors.toMap(id, Function.identity()));
  }

  private static void requireOwned(Collection<Long> requested, Set<Long> existing, String kind) {
    for (Long id : requested)
      if (id > 0 && !existing.contains(id))
        throw ScenarioException.invalid(kind + " " + id + ": 이 버전이 소유한 ID가 아닙니다.");
  }

  public void store(AlcoholAttributeScenarioVersion v, ScenarioDefinition d) {
    var stageRows = byId(stages.findStages(v.getId()), AlcoholAttributeScenarioStage::getId);
    var questionRows =
        byId(questions.findQuestions(v.getId()), AlcoholAttributeScenarioQuestion::getId);
    var rules =
        byId(transitions.findTransitions(v.getId()), AlcoholAttributeScenarioTransition::getId);
    requireOwned(d.stages().stream().map(x -> x.id()).toList(), stageRows.keySet(), "단계");
    requireOwned(d.questions().stream().map(x -> x.id()).toList(), questionRows.keySet(), "질문");
    requireOwned(d.transitions().stream().map(x -> x.id()).toList(), rules.keySet(), "규칙");
    v.startAt(null);
    int n = 10000;
    for (var s : stageRows.values()) s.moveTo(n++);
    for (var q : questionRows.values()) q.moveTo(n++);
    for (var t : rules.values()) t.moveTo(n++);
    // 고유 순서를 임시 영역으로 옮긴 뒤 최종 순서와 신규 ID를 적용한다.
    repository.flush();
    Map<Long, Long> stageIds = new HashMap<>(), questionIds = new HashMap<>();
    for (var s : d.stages()) {
      var entity =
          s.id() > 0
              ? stageRows.get(s.id())
              : AlcoholAttributeScenarioStage.create(v.getId(), s.position());
      entity.moveTo(s.position());
      stages.save(entity);
      stageIds.put(s.id(), entity.getId());
    }
    for (var q : d.questions()) {
      var entity =
          q.id() > 0
              ? questionRows.get(q.id())
              : AlcoholAttributeScenarioQuestion.create(v.getId());
      entity.configure(
          stageIds.get(q.stageId()),
          q.branchOrder(),
          q.title(),
          q.sourceKind(),
          q.attributeId(),
          q.inputKind(),
          q.inputConfig());
      questions.save(entity);
      questionIds.put(q.id(), entity.getId());
    }
    var keptRules = d.transitions().stream().map(t -> t.id()).collect(Collectors.toSet());
    transitions.deleteAll(
        rules.values().stream().filter(t -> !keptRules.contains(t.getId())).toList());
    for (var t : d.transitions()) {
      var entity =
          t.id() > 0 ? rules.get(t.id()) : AlcoholAttributeScenarioTransition.create(v.getId());
      entity.configure(
          questionIds.get(t.fromQuestionId()),
          t.priority(),
          t.predicate(),
          t.targetKind(),
          questionIds.get(t.targetQuestionId()));
      transitions.save(entity);
    }
    // 자식 CASCADE 전에 유지할 이동 참조와 질문의 단계 이동을 반영한다.
    repository.flush();
    var keptQuestions = new HashSet<>(questionIds.values());
    questions.deleteAll(
        questionRows.values().stream().filter(q -> !keptQuestions.contains(q.getId())).toList());
    repository.flush();
    var keptStages = new HashSet<>(stageIds.values());
    stages.deleteAll(
        stageRows.values().stream().filter(s -> !keptStages.contains(s.getId())).toList());
    v.startAt(questionIds.get(d.startQuestionId()));
    repository.flush();
  }
}
