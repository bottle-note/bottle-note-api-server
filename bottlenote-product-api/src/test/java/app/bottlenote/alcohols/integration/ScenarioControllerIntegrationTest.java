package app.bottlenote.alcohols.integration;

import static app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.ScenarioTypes.Operator;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioStageItem;
import app.bottlenote.alcohols.service.ScenarioDefinitionStore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@DisplayName("사용자 시나리오를 실행할 때 현재 후보와 무상태 답변 계약을 검증한다")
class ScenarioControllerIntegrationTest extends IntegrationTestSupport {
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transactions;
  @Autowired AlcoholScenarioRepository scenarios;
  @Autowired AlcoholScenarioVersionRepository versions;
  @Autowired ScenarioDefinitionStore definitions;
  private static final long GROUP = 970001, PRICE = 970001;
  private long scenarioId, versionId;

  @BeforeEach
  void seed() {
    jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?, '사용자 시나리오')", GROUP);
    jdbc.update(
        "INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype,allow_multiple_values) VALUES(?,?,'scenario_price','가격','PRICE','DOUBLE',true)",
        PRICE,
        GROUP);
    jdbc.update(
        "INSERT INTO tasting_tag_groups(id,code,name,display_order) VALUES(970001,'PEAT','피트',0)");
    jdbc.update("INSERT INTO tasting_tags(id,kor_name,eng_name) VALUES(970001,'피트','Peat')");
    jdbc.update(
        "INSERT INTO tasting_tag_group_tags(group_id,tasting_tag_id,role) VALUES(970001,970001,'CORE')");
    for (int i = 1; i <= 8; i++) {
      jdbc.update(
          "INSERT INTO alcohols(id,kor_name,eng_name,type,category_group,abv,kor_category,eng_category,age,cask,volume) VALUES(?,?,?,'WHISKY',?,?,'위스키','Whisky','12','BOURBON','700')",
          980000 + i,
          "후보 " + i,
          "Candidate " + i,
          i <= 4 ? "SINGLE_MALT" : "BOURBON",
          39 + i + "%");
      jdbc.update(
          "INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)",
          980000 + i,
          PRICE,
          i == 3 ? "2000.5" : String.valueOf(i * 1000));
      if (i % 2 == 0)
        jdbc.update(
            "INSERT INTO alcohols_tasting_tags(alcohol_id,tasting_tag_id) VALUES(?,970001)",
            980000 + i);
    }
    transactions.executeWithoutResult(
        status -> {
          var s = scenarios.saveScenario(AlcoholAttributeScenario.create(GROUP, 1, "추천"));
          var v = versions.save(AlcoholAttributeScenarioVersion.create(s.getId(), 1));
          var d =
              new ScenarioDefinition(
                  1,
                  -11L,
                  List.of(
                      new ScenarioStageItem(-1L, 1),
                      new ScenarioStageItem(-2L, 2),
                      new ScenarioStageItem(-3L, 3),
                      new ScenarioStageItem(-4L, 4)),
                  List.of(
                      number(
                          -11, -1, 1, "가격", AlcoholQuestionSource.ATTRIBUTE, PRICE, 0, 10000, 1000),
                      choice(
                          -21,
                          -2,
                          1,
                          "종류",
                          AlcoholQuestionSource.CATEGORY,
                          null,
                          "SINGLE_MALT",
                          "BOURBON"),
                      number(-31, -3, 1, "도수", AlcoholQuestionSource.ABV, null, 0, 100, 1),
                      choice(-41, -4, 1, "향", AlcoholQuestionSource.TASTING_GROUP, null, "PEAT")),
                  List.of(
                      rule(-101, -11, 1, Operator.ALWAYS, null, -21L),
                      rule(-102, -21, 1, Operator.EQ, "BOURBON", null),
                      rule(-103, -21, 2, Operator.ALWAYS, null, -31L),
                      rule(-104, -31, 1, Operator.ALWAYS, null, -41L),
                      rule(-105, -41, 1, Operator.ALWAYS, null, null)));
          definitions.store(v, d);
          v.publish();
          versions.save(v);
          s.activate(v.getId());
          scenarios.saveScenario(s);
          scenarioId = s.getId();
          versionId = v.getId();
        });
  }

  private String url() {
    return "/api/v1/scenarios/" + scenarioId;
  }

  private JsonNode data(MvcTestResult result) throws Exception {
    result.assertThat().hasStatusOk();
    return mapper.readTree(result.getResponse().getContentAsString()).get("data");
  }

  private JsonNode start() throws Exception {
    return data(mockMvcTester.get().uri(url()).exchange());
  }

  private Map<String, Object> answer(JsonNode question, int index) {
    return Map.of(
        "questionId",
        question.get("questionId").asLong(),
        "optionId",
        question.at("/options/" + index + "/optionId").asText());
  }

  private Map<String, Object> any(JsonNode question) {
    return answer(question, question.get("options").size() - 1);
  }

  private MvcTestResult evaluate(List<Map<String, Object>> answers, Map<String, Object> extras)
      throws Exception {
    var body = new HashMap<String, Object>();
    body.put("versionId", versionId);
    body.put("answers", answers);
    body.put("mode", "QUESTION");
    body.putAll(extras);
    return mockMvcTester
        .post()
        .uri(url() + "/evaluate")
        .with(csrf())
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsString(body))
        .exchange();
  }

  @Test
  @DisplayName("로그인 없이 시작할 때 게시본과 숫자 선택지를 반환한다")
  void startsPublishedScenario() throws Exception {
    var s = start();
    assertThat(s.get("versionId").asLong()).isEqualTo(versionId);
    assertThat(s.get("candidateCount").asLong()).isEqualTo(8);
    assertThat(s.get("state").asText()).isEqualTo("QUESTION");
    assertThat(s.at("/question/options").size()).isBetween(2, 5);
    assertThat(s.at("/candidates/content").isEmpty()).isTrue();
    assertThat(s.toString())
        .doesNotContain("attributeKey", "transitions", "lockVersion", "createPrincipal");
  }

  @Test
  @DisplayName("숫자 구간을 선택할 때 소수 후보가 경계 사이에서 빠지거나 겹치지 않는다")
  void numericIntervalsPartitionCandidates() throws Exception {
    var question = start().get("question");
    Set<Long> ids = new HashSet<>();
    long sum = 0;
    for (int i = 0; i < question.get("options").size() - 1; i++) {
      var result = data(evaluate(List.of(answer(question, i)), Map.of("mode", "RESULT")));
      assertThat(result.get("candidateCount").asLong())
          .isEqualTo(question.at("/options/" + i + "/candidateCount").asLong());
      sum += result.get("candidateCount").asLong();
      for (var item : result.at("/candidates/content"))
        assertThat(ids.add(item.get("alcoholId").asLong())).isTrue();
    }
    assertThat(sum).isEqualTo(8);
    assertThat(ids).hasSize(8);
  }

  @Test
  @DisplayName("저장한 숫자 답변으로 다시 평가할 때 구간을 유지하고 변경된 연결을 바로 반영한다")
  void restoredAnswerUsesCurrentMappingsWithoutChangingRange() throws Exception {
    var q = start().get("question");
    var a = answer(q, 0);
    var before = data(evaluate(List.of(a), Map.of("mode", "RESULT")));
    jdbc.update(
        "UPDATE alcohol_attribute_mappings SET attribute_value='9000' WHERE alcohol_id=980001 AND attribute_id=?",
        PRICE);
    var after = data(evaluate(List.of(a), Map.of("mode", "RESULT")));
    assertThat(after.get("candidateCount").asLong())
        .isEqualTo(before.get("candidateCount").asLong() - 1);
    assertThat(after.at("/answers/0/label")).isEqualTo(before.at("/answers/0/label"));
  }

  @Test
  @DisplayName("게시 상태나 활성 버전이 바뀌면 다음 요청에서 즉시 반영한다")
  void visibilityAndVersionChangesApplyImmediately() throws Exception {
    start();
    jdbc.update("UPDATE alcohol_attribute_scenarios SET is_enabled=false WHERE id=?", scenarioId);
    mockMvcTester.get().uri(url()).exchange().assertThat().hasStatus(HttpStatus.NOT_FOUND);
    evaluate(List.of(), Map.of()).assertThat().hasStatus(HttpStatus.NOT_FOUND);
    jdbc.update("UPDATE alcohol_attribute_scenarios SET is_enabled=true WHERE id=?", scenarioId);
    start();
    evaluate(List.of(), Map.of("versionId", versionId + 1))
        .assertThat()
        .hasStatus(HttpStatus.CONFLICT);
    jdbc.update(
        "UPDATE alcohol_attribute_scenario_versions SET status='DRAFT' WHERE id=?", versionId);
    mockMvcTester.get().uri(url()).exchange().assertThat().hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("정의상 종료에 도달하면 중간 종료와 구분해 결과를 반환한다")
  void branchesAndEnds() throws Exception {
    var a = any(start().get("question"));
    var q = data(evaluate(List.of(a), Map.of())).get("question");
    var result = data(evaluate(List.of(a, answer(q, 1)), Map.of()));
    assertThat(result.get("state").asText()).isEqualTo("SCENARIO_END");
    assertThat(result.get("candidateCount").asLong()).isEqualTo(4);
    assertThat(data(evaluate(List.of(a), Map.of("mode", "RESULT"))).get("state").asText())
        .isEqualTo("USER_FINISHED");
  }

  @Test
  @DisplayName("과거 질문을 볼 때 답변은 유지하고 후보에는 직전 답변까지만 적용한다")
  void historicalViewAndEarlyFinishUsePrefix() throws Exception {
    var first = start().get("question");
    var a = any(first);
    var second = data(evaluate(List.of(a), Map.of())).get("question");
    var b = answer(second, 0);
    var past =
        data(evaluate(List.of(a, b), Map.of("viewQuestionId", first.get("questionId").asLong())));
    assertThat(past.get("candidateCount").asLong()).isEqualTo(8);
    assertThat(past.get("answers").size()).isEqualTo(2);
    assertThat(past.at("/answers/0/applied").asBoolean()).isFalse();
    var result =
        data(
            evaluate(
                List.of(a, b),
                Map.of("viewQuestionId", second.get("questionId").asLong(), "mode", "RESULT")));
    assertThat(result.get("candidateCount").asLong()).isEqualTo(8);
    assertThat(data(evaluate(List.of(a), Map.of())).at("/question/questionId"))
        .isEqualTo(second.get("questionId"));
  }

  @Test
  @DisplayName("도수와 맛 그룹을 선택할 때 후보 수와 실제 결과가 일치한다")
  void abvAndTastingGroupMatchResults() throws Exception {
    var a = any(start().get("question"));
    var q2 = data(evaluate(List.of(a), Map.of())).get("question");
    var b = answer(q2, 0);
    var q3 = data(evaluate(List.of(a, b), Map.of())).get("question");
    for (int i = 0; i < q3.get("options").size() - 1; i++) {
      var part = data(evaluate(List.of(a, b, answer(q3, i)), Map.of("mode", "RESULT")));
      assertThat(part.get("candidateCount").asLong())
          .isEqualTo(q3.at("/options/" + i + "/candidateCount").asLong());
    }
    var c = any(q3);
    var q4 = data(evaluate(List.of(a, b, c), Map.of())).get("question");
    assertThat(q4.at("/options/0/candidateCount").asLong()).isEqualTo(2);
    var done = data(evaluate(List.of(a, b, c, answer(q4, 0)), Map.of()));
    assertThat(done.get("candidateCount").asLong()).isEqualTo(2);
    assertThat(done.get("state").asText()).isEqualTo("SCENARIO_END");
    jdbc.update("DELETE FROM alcohols_tasting_tags WHERE alcohol_id=980002");
    assertThat(
            data(evaluate(List.of(a, b, c, answer(q4, 0)), Map.of()))
                .get("candidateCount")
                .asLong())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("선택지나 질문 순서를 위조하면 답변을 거절한다")
  void rejectsTamperingDuplicatesAndInvalidPath() throws Exception {
    var first = start().get("question");
    var a = any(first);
    var second = data(evaluate(List.of(a), Map.of())).get("question");
    var b = answer(second, 0);
    evaluate(List.of(b), Map.of()).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(List.of(a, a), Map.of()).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(
            List.of(Map.of("questionId", a.get("questionId"), "optionId", a.get("optionId") + "x")),
            Map.of())
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(List.of(a, b), Map.of("viewQuestionId", 999999))
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
    var changed = answer(first, 0);
    evaluate(List.of(changed, b), Map.of()).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("결과를 추가 조회할 때 중복 없이 순회하고 다른 조건의 커서를 거절한다")
  void keysetPaginationIsBoundToAnswerContext() throws Exception {
    var first = data(evaluate(List.of(), Map.of("mode", "RESULT", "size", 3)));
    String cursor = first.at("/candidates/pagination/nextCursor").asText();
    var second = data(evaluate(List.of(), Map.of("mode", "RESULT", "size", 3, "cursor", cursor)));
    assertThat(first.at("/candidates/content/2/alcoholId").asLong())
        .isLessThan(second.at("/candidates/content/0/alcoholId").asLong());
    evaluate(List.of(), Map.of("mode", "RESULT", "size", 4, "cursor", cursor))
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(
            List.of(any(start().get("question"))),
            Map.of("mode", "RESULT", "size", 3, "cursor", cursor))
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(List.of(), Map.of("cursor", cursor)).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("후보가 없거나 단일 값이면 빈 구간을 발급하지 않는다")
  void handlesEmptyAndIdenticalDistribution() throws Exception {
    jdbc.update(
        "UPDATE alcohol_attribute_mappings SET attribute_value='1000' WHERE attribute_id=?", PRICE);
    var q = start().get("question");
    assertThat(q.get("options").size()).isEqualTo(2);
    jdbc.update("DELETE FROM alcohol_attribute_mappings WHERE attribute_id=?", PRICE);
    var s = start();
    assertThat(s.get("candidateCount").asLong()).isZero();
    assertThat(s.at("/question/options").size()).isEqualTo(1);
    assertThat(s.at("/question/options/0/selectable").asBoolean()).isFalse();
  }

  @Test
  @DisplayName("숫자 선택형은 값의 소수 표기와 무관하게 정확히 일치한다")
  void numericChoiceMatchesExactly() throws Exception {
    long questionId = start().at("/question/questionId").asLong();
    String config =
        "{\"allowAny\":true,\"choices\":[{\"id\":\"two\",\"label\":\"이천 원\",\"value\":2000.0}]}";
    jdbc.update(
        "UPDATE alcohol_attribute_scenario_questions SET input_kind='SINGLE_CHOICE', input_config=? WHERE id=?",
        config,
        questionId);
    var question = start().get("question");
    assertThat(question.at("/options/0/candidateCount").asLong()).isEqualTo(1);
    assertThat(
            data(evaluate(List.of(answer(question, 0)), Map.of("mode", "RESULT")))
                .get("candidateCount")
                .asLong())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("같은 속성의 이전 범위를 만족하는 값만 다음 숫자 선택지에 집계한다")
  void repeatedAttributeUsesSameMappingValue() throws Exception {
    jdbc.update(
        "INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(980001,?,'9000')",
        PRICE);
    var first = start().get("question");
    var a = answer(first, 0);
    long secondId = data(evaluate(List.of(a), Map.of())).at("/question/questionId").asLong();
    String config = "{\"allowAny\":true,\"min\":0,\"max\":10000,\"step\":1000}";
    // 이 테스트의 두 번째 질문은 숫자 질문이므로 선택형 분기를 제거한다.
    jdbc.update(
        "DELETE FROM alcohol_attribute_scenario_transitions WHERE from_question_id=? AND priority=1",
        secondId);
    jdbc.update(
        "UPDATE alcohol_attribute_scenario_questions SET source_kind='ATTRIBUTE', attribute_id=?, input_kind='NUMBER_RANGE',input_config=? WHERE id=?",
        PRICE,
        config,
        secondId);
    var q = data(evaluate(List.of(a), Map.of())).get("question");
    long total = 0;
    for (int i = 0; i < q.get("options").size() - 1; i++) {
      var result = data(evaluate(List.of(a, answer(q, i)), Map.of("mode", "RESULT")));
      long count = q.at("/options/" + i + "/candidateCount").asLong();
      assertThat(result.get("candidateCount").asLong()).isEqualTo(count);
      total += count;
    }
    assertThat(total).isEqualTo(first.at("/options/0/candidateCount").asLong());
  }

  @Test
  @DisplayName("다른 시나리오의 선택지를 거절하고 참여 이벤트를 자동 저장하지 않는다")
  void isolatesScenarioAndDoesNotRecordParticipation() throws Exception {
    var a = any(start().get("question"));
    data(evaluate(List.of(a), Map.of("mode", "RESULT")));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_content_events", Long.class))
        .isZero();
    long other =
        transactions.execute(
            status -> {
              var s = scenarios.saveScenario(AlcoholAttributeScenario.create(GROUP, 2, "다른 추천"));
              var v = versions.save(AlcoholAttributeScenarioVersion.create(s.getId(), 1));
              definitions.store(
                  v, definitions.read(versions.findById(versionId).orElseThrow()).forClone());
              v.publish();
              versions.save(v);
              s.activate(v.getId());
              scenarios.saveScenario(s);
              return s.getId();
            });
    scenarioId = other;
    versionId =
        jdbc.queryForObject(
            "SELECT active_version_id FROM alcohol_attribute_scenarios WHERE id=?",
            Long.class,
            other);
    start();
    evaluate(List.of(a), Map.of()).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("요청 필드가 빠지거나 범위를 벗어나면 400을 반환한다")
  void validatesPublicRequest() throws Exception {
    evaluate(List.of(), Map.of("size", 101)).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(List.of(), Map.of("mode", "UNKNOWN")).assertThat().hasStatus(HttpStatus.BAD_REQUEST);
    evaluate(List.of(Map.of("questionId", -1, "optionId", "x")), Map.of())
        .assertThat()
        .hasStatus(HttpStatus.BAD_REQUEST);
  }
}
