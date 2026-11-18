package app.external.systemone.openai;

import static org.assertj.core.api.Assertions.assertThat;

import app.external.systemone.ExplosiveState;
import app.external.systemone.StubProviderServer;
import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneAnswer;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneResult.Failure;
import app.external.systemone.dto.response.SystemOneResult.Success;
import app.external.systemone.http.SystemOneHttpTransport;
import app.external.systemone.openai.config.OpenAiDecisionsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClient;

/** 로컬 HTTP 서버로 OpenAI Decisions 계약을 재현해 실제 RestClient 전송 경로까지 검증한다. */
@Tag("unit")
@DisplayName("DefaultOpenAiSystemOneClient 단위 테스트")
class DefaultOpenAiSystemOneClientTest {
  private static final String API_KEY = "test-openai-key";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private StubProviderServer server;

  @BeforeEach
  void setUp() throws IOException {
    server = new StubProviderServer(DefaultOpenAiSystemOneClient.DECISIONS_PATH);
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  @Nested
  @DisplayName("성공 응답")
  class SuccessCase {

    @Test
    @DisplayName("choice, score, noul 질문을 Decisions 계약으로 보내고 응답을 공급자 중립 타입으로 변환한다")
    void evaluate_세_가지_질문을_변환할_수_있다() throws IOException {
      // given
      server.respond(
          200,
          """
          {
            "id": "dec_123",
            "model": "gpt-6-luna",
            "answers": [
              {
                "type": "choice",
                "name": "department",
                "choice": "technical",
                "probabilities": [
                  {"value": "billing", "probability": 0.08},
                  {"value": "technical", "probability": 0.85},
                  {"value": "sales", "probability": 0.07}
                ],
                "confidence": 0.82
              },
              {
                "type": "score",
                "name": "sentiment",
                "score": 1.56,
                "probabilities": [
                  {"value": 0, "label": "Negative", "probability": 0.1},
                  {"value": 1, "label": "Mixed", "probability": 0.24},
                  {"value": 2, "label": "Positive", "probability": 0.66}
                ],
                "confidence": 0.63
              },
              {"type": "predicate", "name": "urgent", "probability": 0.91}
            ],
            "usage": {"input_tokens": 312, "output_tokens": 0}
          }
          """);
      Map<String, SystemOneQuestion> questions = new LinkedHashMap<>();
      questions.put(
          "department",
          new SystemOneQuestion.Choice(
              "Which team should handle this?",
              orderedMap("billing", "Payments", "technical", "Bugs", "sales", "Pricing")));
      questions.put(
          "sentiment",
          new SystemOneQuestion.Score(
              "How positive is this?", List.of("Negative", "Mixed", "Positive")));
      questions.put("urgent", new SystemOneQuestion.Noul("Does this convey urgency?"));

      // when
      SystemOneResult result =
          client(properties()).evaluate(new SystemOneRequest("결제가 안 돼요", questions));

      // then
      assertThat(result).isInstanceOf(Success.class);
      Success success = (Success) result;
      assertThat(success.model()).isEqualTo("gpt-6-luna");
      assertThat(success.usage().inputTokens()).isEqualTo(312);

      SystemOneAnswer.Choice department =
          success.answer("department", SystemOneAnswer.Choice.class);
      assertThat(department.choice()).isEqualTo("technical");
      assertThat(department.probabilities())
          .containsExactly(
              Map.entry("billing", 0.08), Map.entry("technical", 0.85), Map.entry("sales", 0.07));
      assertThat(department.confidence()).isEqualTo(0.82);

      SystemOneAnswer.Score sentiment = success.answer("sentiment", SystemOneAnswer.Score.class);
      assertThat(sentiment.score()).isEqualTo(1.56);
      assertThat(sentiment.legend())
          .containsExactly(
              Map.entry("0", "Negative"), Map.entry("1", "Mixed"), Map.entry("2", "Positive"));
      assertThat(sentiment.probabilities())
          .containsExactly(Map.entry("0", 0.1), Map.entry("1", 0.24), Map.entry("2", 0.66));

      assertThat(success.answer("urgent", SystemOneAnswer.Noul.class).probability())
          .isEqualTo(0.91);

      assertThat(server.lastAuthorization()).isEqualTo("Bearer " + API_KEY);
      JsonNode sent = objectMapper.readTree(server.lastRequestBody());
      assertThat(sent.path("model").asText()).isEqualTo("gpt-6-luna");
      assertThat(sent.path("input").asText()).isEqualTo("결제가 안 돼요");
      assertThat(sent.path("questions").isArray()).isTrue();
      assertThat(sent.at("/questions/0/type").asText()).isEqualTo("choice");
      assertThat(sent.at("/questions/0/name").asText()).isEqualTo("department");
      assertThat(sent.at("/questions/0/choices/1/value").asText()).isEqualTo("technical");
      assertThat(sent.at("/questions/0/choices/1/description").asText()).isEqualTo("Bugs");
      assertThat(sent.at("/questions/0").has("levels")).isFalse();
      assertThat(sent.at("/questions/1/type").asText()).isEqualTo("score");
      assertThat(sent.at("/questions/1/levels/2/label").asText()).isEqualTo("Positive");
      assertThat(sent.at("/questions/1/levels/2/description").asText()).isEqualTo("Positive");
      assertThat(sent.at("/questions/2/type").asText()).isEqualTo("predicate");
      assertThat(sent.at("/questions/2/name").asText()).isEqualTo("urgent");
      assertThat(sent.at("/questions/2").has("choices")).isFalse();
      assertThat(sent.at("/questions/2").has("levels")).isFalse();
    }

    @Test
    @DisplayName("문자열이 아닌 state는 JSON 텍스트로 직렬화해 input에 싣는다")
    void evaluate_객체_state를_JSON_텍스트로_보낼_수_있다() throws IOException {
      server.respond(200, predicateBody(0.7));

      SystemOneResult result =
          client(properties())
              .evaluate(
                  SystemOneRequest.of(
                      Map.of("text", "지금 바로 확인해 주세요"),
                      "urgent",
                      new SystemOneQuestion.Noul("Does this convey urgency?")));

      assertThat(result).isInstanceOf(Success.class);
      JsonNode sent = objectMapper.readTree(server.lastRequestBody());
      assertThat(sent.path("input").isTextual()).isTrue();
      assertThat(objectMapper.readTree(sent.path("input").asText()).path("text").asText())
          .isEqualTo("지금 바로 확인해 주세요");
    }
  }

  @Nested
  @DisplayName("실패 응답")
  class FailureCase {

    @ParameterizedTest(name = "HTTP {0} -> {1}")
    @CsvSource({
      "400, INVALID_REQUEST",
      "401, UNAUTHORIZED",
      "403, UNAUTHORIZED",
      "429, RATE_LIMITED",
      "500, PROVIDER_UNAVAILABLE",
      "503, PROVIDER_UNAVAILABLE",
      "404, INVALID_RESPONSE"
    })
    @DisplayName("비정상 HTTP 상태를 실패 유형으로 변환하고 OpenAI 오류 본문의 메시지를 담는다")
    void evaluate_비정상_상태를_실패로_변환할_수_있다(int status, SystemOneFailureType expected) {
      server.respond(
          status,
          "{\"error\":{\"message\":\"provider said no\",\"type\":\"invalid_request_error\"}}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(expected);
      assertThat(failure.httpStatus()).isEqualTo(status);
      assertThat(failure.message()).isEqualTo("provider said no");
    }

    @Test
    @DisplayName("답변 중 하나라도 refusal이면 REFUSED 실패를 반환하고 거부된 질문 키를 담는다")
    void evaluate_거부_응답을_처리할_수_있다() {
      server.respond(
          200,
          """
          {
            "model": "gpt-6-luna",
            "answers": [
              {"type": "predicate", "name": "urgent", "probability": 0.5},
              {"type": "refusal", "name": "safe"}
            ]
          }
          """);
      Map<String, SystemOneQuestion> questions = new LinkedHashMap<>();
      questions.put("urgent", new SystemOneQuestion.Noul("Does this convey urgency?"));
      questions.put("safe", new SystemOneQuestion.Noul("Is this safe?"));

      Failure failure =
          failure(client(properties()).evaluate(new SystemOneRequest("state", questions)));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.REFUSED);
      assertThat(failure.retryable()).isFalse();
      assertThat(failure.httpStatus()).isEqualTo(200);
      assertThat(failure.message()).contains("safe").doesNotContain("urgent");
    }

    @ParameterizedTest(name = "answers 순서: {0}")
    @CsvSource(
        delimiter = '|',
        value = {
          "refusal 먼저|{\"type\":\"refusal\",\"name\":\"urgent\"},{\"type\":\"predicate\",\"name\":\"urgent\",\"probability\":0.9}",
          "predicate 먼저|{\"type\":\"predicate\",\"name\":\"urgent\",\"probability\":0.9},{\"type\":\"refusal\",\"name\":\"urgent\"}"
        })
    @DisplayName("같은 name의 답변이 두 번 오면 순서와 무관하게 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_중복된_답변_이름을_거부할_수_있다(String ignoredOrder, String answers) {
      server.respond(200, "{\"model\":\"gpt-6-luna\",\"answers\":[" + answers + "]}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
      assertThat(failure.message()).contains("urgent");
    }

    @Test
    @DisplayName("state를 JSON으로 직렬화할 수 없으면 요청을 보내지 않고 예외 원문 없는 INVALID_REQUEST를 반환한다")
    void evaluate_직렬화_불가_state를_처리할_수_있다() {
      SystemOneRequest request =
          SystemOneRequest.of(new ExplosiveState(), "urgent", new SystemOneQuestion.Noul("q"));

      Failure failure = failure(client(properties()).evaluate(request));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_REQUEST);
      assertThat(failure.message()).doesNotContain(ExplosiveState.SECRET);
      assertThat(server.requestCount()).isZero();
    }

    @Test
    @DisplayName("응답이 readTimeout보다 늦으면 TIMEOUT 실패를 반환한다")
    void evaluate_응답_지연을_타임아웃으로_처리할_수_있다() {
      server.respondAfter(Duration.ofMillis(1500), 200, predicateBody(0.7));
      OpenAiDecisionsProperties properties = properties();
      properties.setReadTimeout(Duration.ofMillis(200));

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.TIMEOUT);
    }

    @Test
    @DisplayName("200이지만 JSON이 아니면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_해석_불가_응답을_처리할_수_있다() {
      server.respond(200, "not-json");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("요청한 질문 이름의 답변이 없으면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_누락된_응답을_처리할_수_있다() {
      server.respond(200, "{\"model\":\"gpt-6-luna\",\"answers\":[]}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
      assertThat(failure.message()).contains("urgent");
    }

    @Test
    @DisplayName("응답 타입이 질문 타입과 다르면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_타입이_다른_응답을_처리할_수_있다() {
      server.respond(
          200, "{\"answers\":[{\"type\":\"choice\",\"name\":\"urgent\",\"probability\":0.5}]}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("probabilities 항목에 value가 없으면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_value_없는_확률을_처리할_수_있다() {
      server.respond(
          200,
          """
          {
            "answers": [
              {
                "type": "choice",
                "name": "department",
                "choice": "a",
                "probabilities": [{"probability": 1.0}],
                "confidence": 1.0
              }
            ]
          }
          """);
      SystemOneRequest request =
          SystemOneRequest.of(
              "state",
              "department",
              new SystemOneQuestion.Choice("q", orderedMap("a", "A", "b", "B")));

      Failure failure = failure(client(properties()).evaluate(request));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
      assertThat(failure.message()).contains("department");
    }

    @Test
    @DisplayName("API Key가 없으면 요청을 보내지 않고 NOT_CONFIGURED를 반환한다")
    void evaluate_API_Key_미설정을_처리할_수_있다() {
      OpenAiDecisionsProperties properties = properties();
      properties.setApiKey(null);

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.NOT_CONFIGURED);
      assertThat(server.requestCount()).isZero();
    }
  }

  private DefaultOpenAiSystemOneClient client(OpenAiDecisionsProperties properties) {
    return new DefaultOpenAiSystemOneClient(
        SystemOneHttpTransport.create(
            RestClient.builder(), properties, DefaultOpenAiSystemOneClient.DECISIONS_PATH),
        new OpenAiDecisionsMapper(objectMapper),
        properties,
        objectMapper);
  }

  private OpenAiDecisionsProperties properties() {
    OpenAiDecisionsProperties properties = new OpenAiDecisionsProperties();
    properties.setBaseUrl(server.baseUrl());
    properties.setApiKey(API_KEY);
    properties.setConnectTimeout(Duration.ofSeconds(1));
    properties.setReadTimeout(Duration.ofSeconds(2));
    return properties;
  }

  private static SystemOneRequest noulRequest() {
    return SystemOneRequest.of(
        "지금 바로 확인해 주세요", "urgent", new SystemOneQuestion.Noul("Does this convey urgency?"));
  }

  private static String predicateBody(double probability) {
    return "{\"model\":\"gpt-6-luna\",\"answers\":[{\"type\":\"predicate\",\"name\":\"urgent\",\"probability\":%s}]}"
        .formatted(probability);
  }

  private static Failure failure(SystemOneResult result) {
    assertThat(result).isInstanceOf(Failure.class);
    return (Failure) result;
  }

  private static Map<String, String> orderedMap(String... keyValues) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put(keyValues[i], keyValues[i + 1]);
    }
    return map;
  }
}
