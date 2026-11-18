package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.ScenarioException.invalid;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.ScenarioTypes.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import app.bottlenote.alcohols.exception.AlcoholException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ScenarioDefinitionValidator {
  public void validate(
      ScenarioDefinition d,
      Long groupId,
      Map<Long, AlcoholAttribute> attributes,
      Set<String> tastingCodes,
      boolean publishing) {
    if (d == null || !Integer.valueOf(1).equals(d.definitionSchemaVersion()))
      throw invalid("definitionSchemaVersion은 1만 지원합니다.");
    if (d.stages() == null
        || d.questions() == null
        || d.transitions() == null
        || d.stages().size() > 50
        || d.questions().size() > 100
        || d.transitions().size() > 500) throw invalid("단계/질문/규칙 배열과 크기를 확인해 주세요.");
    var stages = index(d.stages(), ScenarioStageItem::id, "단계");
    var questions = index(d.questions(), ScenarioQuestionItem::id, "질문");
    index(d.transitions(), ScenarioTransitionItem::id, "규칙");
    Set<Integer> positions = new HashSet<>();
    for (var s : d.stages())
      if (!order(s.position(), 1000) || !positions.add(s.position()))
        throw invalid("단계 " + s.id() + ": position은 1~1000이며 중복할 수 없습니다.");
    Set<String> branches = new HashSet<>();
    for (var q : d.questions()) {
      String at = "질문 " + q.id() + ": ";
      if (!stages.containsKey(q.stageId())) throw invalid(at + "같은 버전의 단계가 필요합니다.");
      if (!order(q.branchOrder(), 100) || !branches.add(q.stageId() + ":" + q.branchOrder()))
        throw invalid(at + "branchOrder는 단계 안에서 고유한 1~100입니다.");
      if (q.title() == null
          || q.title().isBlank()
          || q.title().trim().length() > 200
          || q.sourceKind() == null
          || q.inputKind() == null) throw invalid(at + "제목·출처·입력 종류를 확인해 주세요.");
      if ((q.sourceKind() == AlcoholQuestionSource.ATTRIBUTE) != (q.attributeId() != null))
        throw invalid(at + "ATTRIBUTE만 attributeId를 사용합니다.");
      AlcoholAttribute attribute = q.attributeId() == null ? null : attributes.get(q.attributeId());
      if (q.sourceKind() == AlcoholQuestionSource.ATTRIBUTE
          && (attribute == null || !Objects.equals(groupId, attribute.getGroupId())))
        throw invalid(at + "현재 그룹의 속성을 참조해야 합니다.");
      validateInput(q, attribute, tastingCodes, publishing);
    }
    if (d.startQuestionId() != null && !questions.containsKey(d.startQuestionId()))
      throw invalid("startQuestionId는 같은 버전의 질문이어야 합니다.");
    Set<String> priorities = new HashSet<>();
    for (var t : d.transitions()) {
      String at = "규칙 " + t.id() + " (질문 " + t.fromQuestionId() + "): ";
      var q = questions.get(t.fromQuestionId());
      if (q == null) throw invalid(at + "같은 버전의 출발 질문이 필요합니다.");
      if (!order(t.priority(), 1000) || !priorities.add(t.fromQuestionId() + ":" + t.priority()))
        throw invalid(at + "priority는 출발 질문 안에서 고유한 1~1000입니다.");
      if (t.targetKind() == null
          || (t.targetKind() == Target.QUESTION) != (t.targetQuestionId() != null)
          || (t.targetQuestionId() != null && !questions.containsKey(t.targetQuestionId())))
        throw invalid(at + "같은 버전의 QUESTION 또는 명시적 END가 필요합니다.");
      validatePredicate(q, t);
      if (publishing
          && t.targetKind() == Target.QUESTION
          && stages.get(questions.get(t.targetQuestionId()).stageId()).position()
              <= stages.get(q.stageId()).position())
        throw invalid(at + "같은 단계·뒤로 가는 이동·순환은 게시할 수 없습니다.");
    }
    if (publishing) validateGraph(d, stages, questions);
  }

  private static <T> Map<Long, T> index(List<T> values, Function<T, Long> id, String kind) {
    Map<Long, T> map = new LinkedHashMap<>();
    for (T value : values) {
      if (value == null
          || id.apply(value) == null
          || id.apply(value) == 0
          || map.putIfAbsent(id.apply(value), value) != null)
        throw invalid(kind + " ID는 0이 아닌 고유한 값이어야 합니다.");
    }
    return map;
  }

  private static boolean order(Integer value, int max) {
    return value != null && value > 0 && value <= max;
  }

  private void validateInput(
      ScenarioQuestionItem q, AlcoholAttribute a, Set<String> codes, boolean strict) {
    String at = "질문 " + q.id() + ": ";
    var c = q.inputConfig();
    if (c == null || c.allowAny() == null || (c.unit() != null && c.unit().length() > 20))
      throw invalid(at + "inputConfig.allowAny와 올바른 unit이 필요합니다.");
    if (q.inputKind() == Input.SINGLE_CHOICE) {
      if (c.min() != null || c.max() != null || c.step() != null || c.unit() != null)
        throw invalid(at + "선택형은 choices만 사용합니다.");
      if (c.choices() == null || c.choices().size() > 100 || (strict && c.choices().isEmpty()))
        throw invalid(at + "선택지는 게시할 때 1~100개가 필요합니다.");
      Set<String> ids = new HashSet<>();
      Set<String> values = new HashSet<>();
      for (var choice : c.choices()) {
        if (choice == null
            || choice.id() == null
            || !choice.id().matches("[a-zA-Z0-9_-]{1,64}")
            || !ids.add(choice.id())
            || choice.label() == null
            || choice.label().isBlank()
            || choice.label().length() > 200) throw invalid(at + "선택지 ID와 문구를 확인해 주세요.");
        String value = choiceValue(q, a, codes, choice.value());
        if (!values.add(value)) throw invalid(at + "서로 다른 선택지에 같은 값이 중복됩니다.");
      }
    } else {
      if (c.choices() != null
          || (q.sourceKind() != AlcoholQuestionSource.ABV
              && (a == null || !a.getDatatype().isNumeric())))
        throw invalid(at + "NUMBER_RANGE는 숫자 속성 또는 ABV에만 사용합니다.");
      if (strict && (c.min() == null || c.max() == null || c.step() == null))
        throw invalid(at + "숫자 범위의 min/max/step이 필요합니다.");
      for (var n : Arrays.asList(c.min(), c.max(), c.step())) if (n != null) numeric(q, a, n);
      if (c.step() != null && c.step().signum() <= 0) throw invalid(at + "step은 양수여야 합니다.");
      if (c.min() != null && c.max() != null && c.min().compareTo(c.max()) > 0)
        throw invalid(at + "min은 max를 넘을 수 없습니다.");
      if (c.min() != null
          && c.max() != null
          && c.step() != null
          && c.max().subtract(c.min()).remainder(c.step()).signum() != 0)
        throw invalid(at + "max-min은 step의 정수배여야 합니다.");
    }
  }

  public String choiceValue(
      ScenarioQuestionItem q, AlcoholAttribute a, Set<String> codes, JsonNode value) {
    String at = "질문 " + q.id() + ": ";
    if (value == null || value.isNull() || !value.isValueNode())
      throw invalid(at + "선택지 value는 출처에 맞는 JSON scalar여야 합니다.");
    switch (q.sourceKind()) {
      case CATEGORY -> {
        if (!value.isTextual()) throw invalid(at + "CATEGORY는 코드 문자열이 필요합니다.");
        try {
          AlcoholCategoryGroup.valueOf(value.textValue());
        } catch (IllegalArgumentException e) {
          throw invalid(at + "지원하지 않는 CATEGORY 코드입니다.");
        }
      }
      case TASTING_GROUP -> {
        if (!value.isTextual() || !codes.contains(value.textValue()))
          throw invalid(at + "존재하는 TASTING_GROUP 코드가 필요합니다.");
      }
      case ABV -> {
        if (!value.isNumber()) throw invalid(at + "ABV는 JSON 숫자여야 합니다.");
        numeric(q, null, value.decimalValue());
      }
      case ATTRIBUTE -> {
        if (a == null
            || switch (a.getDatatype()) {
              case STRING -> !value.isTextual();
              case BOOLEAN -> !value.isBoolean();
              case INT -> !value.isIntegralNumber();
              case DOUBLE -> !value.isNumber();
            }) throw invalid(at + "선택값의 JSON 타입이 속성 자료형과 다릅니다.");
        try {
          String normalized =
              AlcoholAttributeMapping.normalizeValue(a.getDatatype(), value.asText());
          a.validateValue(normalized);
          if (!normalized.equals(value.asText()) && a.getDatatype() == AttributeDatatype.STRING)
            throw invalid(at + "선택값의 앞뒤 공백을 제거해 주세요.");
          return normalized;
        } catch (AlcoholException e) {
          throw invalid(at + "속성의 허용값·형식에 맞지 않는 선택값입니다.");
        }
      }
    }
    return value.isNumber()
        ? value.decimalValue().stripTrailingZeros().toPlainString()
        : value.asText();
  }

  private void numeric(ScenarioQuestionItem q, AlcoholAttribute a, BigDecimal number) {
    String at = "질문 " + q.id() + ": ";
    if (number.precision() > 21 || number.scale() > 6) throw invalid(at + "숫자는 최대 소수 6자리까지 지원합니다.");
    if (q.sourceKind() == AlcoholQuestionSource.ABV) {
      if (number.signum() < 0 || number.compareTo(new BigDecimal("100")) > 0)
        throw invalid(at + "ABV는 0~100입니다.");
    } else if (a != null) {
      try {
        AlcoholAttributeMapping.normalizeValue(
            a.getDatatype(), number.stripTrailingZeros().toPlainString());
      } catch (AlcoholException e) {
        throw invalid(at + "숫자가 속성 자료형의 범위를 벗어납니다.");
      }
    }
  }

  private void validatePredicate(ScenarioQuestionItem q, ScenarioTransitionItem t) {
    String at = "규칙 " + t.id() + " (질문 " + q.id() + "): ";
    var p = t.predicate();
    if (p == null || p.operator() == null) throw invalid(at + "predicate.operator가 필요합니다.");
    switch (p.operator()) {
      case ALWAYS -> {
        if (p.value() != null && !p.value().isNull())
          throw invalid(at + "ALWAYS는 value를 사용하지 않습니다.");
      }
      case EQ -> {
        if (q.inputKind() != Input.SINGLE_CHOICE
            || p.value() == null
            || !p.value().isTextual()
            || q.inputConfig().choices().stream()
                .noneMatch(c -> c.id().equals(p.value().textValue())))
          throw invalid(at + "EQ는 이 질문의 choiceId 문자열을 비교합니다.");
      }
      case GTE, LTE -> {
        if (q.inputKind() != Input.NUMBER_RANGE
            || p.value() == null
            || !p.value().isNumber()
            || p.value().decimalValue().precision() > 21
            || p.value().decimalValue().scale() > 6)
          throw invalid(at + "GTE/LTE는 범위형 질문과 숫자 value가 필요합니다.");
      }
    }
  }

  private void validateGraph(
      ScenarioDefinition d,
      Map<Long, ScenarioStageItem> stages,
      Map<Long, ScenarioQuestionItem> questions) {
    if (d.startQuestionId() == null || questions.isEmpty()) throw invalid("게시에는 시작 질문과 질문이 필요합니다.");
    for (var s : stages.values())
      if (questions.values().stream().noneMatch(q -> q.stageId().equals(s.id())))
        throw invalid("단계 " + s.id() + ": 비어 있는 단계는 게시할 수 없습니다.");
    var outgoing =
        d.transitions().stream()
            .collect(Collectors.groupingBy(ScenarioTransitionItem::fromQuestionId));
    Map<Long, List<ScenarioTransitionItem>> possible = new HashMap<>();
    for (var q : questions.values()) {
      var rules =
          outgoing.getOrDefault(q.id(), List.of()).stream()
              .sorted(Comparator.comparing(ScenarioTransitionItem::priority))
              .toList();
      if (rules.isEmpty()
          || rules.getLast().predicate().operator() != Operator.ALWAYS
          || rules.stream().filter(t -> t.predicate().operator() == Operator.ALWAYS).count() != 1)
        throw invalid("질문 " + q.id() + ": 출발 규칙의 마지막에는 ALWAYS가 정확히 한 개 필요합니다.");
      possible.put(q.id(), possibleRules(q, rules));
    }
    Set<Long> reached = new HashSet<>();
    Deque<Long> queue = new ArrayDeque<>();
    queue.add(d.startQuestionId());
    while (!queue.isEmpty()) {
      Long id = queue.remove();
      if (!reached.add(id)) continue;
      for (var t : possible.get(id))
        if (t.targetKind() == Target.QUESTION) queue.add(t.targetQuestionId());
    }
    for (var q : questions.values())
      if (!reached.contains(q.id())) throw invalid("질문 " + q.id() + ": 시작 질문에서 도달할 수 없습니다.");
    // 엄격한 전방 이동과 모든 질문의 마지막 ALWAYS가 모든 경로의 명시적 END를 보장한다.
  }

  private List<ScenarioTransitionItem> possibleRules(
      ScenarioQuestionItem q, List<ScenarioTransitionItem> rules) {
    List<ScenarioTransitionItem> result = new ArrayList<>();
    Set<String> remaining =
        q.inputKind() == Input.SINGLE_CHOICE
            ? q.inputConfig().choices().stream()
                .map(ScenarioInputConfig.ScenarioChoice::id)
                .collect(Collectors.toSet())
            : new HashSet<>();
    BigDecimal upper = q.inputConfig().max(), lower = q.inputConfig().min();
    for (var t : rules) {
      boolean possible;
      switch (t.predicate().operator()) {
        case ALWAYS ->
            possible =
                q.inputConfig().allowAny()
                    || (q.inputKind() == Input.SINGLE_CHOICE
                        ? !remaining.isEmpty()
                        : upper.compareTo(q.inputConfig().min()) >= 0
                            && lower.compareTo(q.inputConfig().max()) <= 0);
        case EQ -> possible = remaining.remove(t.predicate().value().textValue());
        case GTE -> {
          var edge = aligned(q, t.predicate().value().decimalValue(), RoundingMode.CEILING);
          possible =
              edge.compareTo(upper) <= 0
                  && upper.compareTo(q.inputConfig().min()) >= 0
                  && edge.compareTo(q.inputConfig().max()) <= 0
                  && lower.compareTo(q.inputConfig().max()) <= 0;
          upper = upper.min(edge.subtract(q.inputConfig().step()));
        }
        case LTE -> {
          var edge = aligned(q, t.predicate().value().decimalValue(), RoundingMode.FLOOR);
          possible =
              edge.compareTo(lower) >= 0
                  && lower.compareTo(q.inputConfig().max()) <= 0
                  && edge.compareTo(q.inputConfig().min()) >= 0
                  && upper.compareTo(q.inputConfig().min()) >= 0;
          lower = lower.max(edge.add(q.inputConfig().step()));
        }
        default -> throw invalid("미지원 조건입니다.");
      }
      if (!possible && t.predicate().operator() != Operator.ALWAYS)
        throw invalid("규칙 " + t.id() + " (질문 " + q.id() + "): 중복·선행 조건에 가려지거나 불가능한 조건입니다.");
      if (possible) result.add(t);
    }
    return result;
  }

  private static BigDecimal aligned(ScenarioQuestionItem q, BigDecimal value, RoundingMode mode) {
    var c = q.inputConfig();
    return value.subtract(c.min()).divide(c.step(), 0, mode).multiply(c.step()).add(c.min());
  }
}
