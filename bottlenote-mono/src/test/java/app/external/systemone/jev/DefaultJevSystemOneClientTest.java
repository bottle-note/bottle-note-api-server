package app.external.systemone.jev;

import static org.assertj.core.api.Assertions.assertThat;

import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneAnswer;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneResult.Failure;
import app.external.systemone.dto.response.SystemOneResult.Success;
import app.external.systemone.jev.config.JevProperties;
import app.external.systemone.jev.v1.JevV1Mapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClient;

/** 로컬 HTTP 서버로 Jev v1 계약을 재현해 실제 RestClient 전송 경로까지 검증한다. */
@Tag("unit")
@DisplayName("DefaultJevSystemOneClient 단위 테스트")
class DefaultJevSystemOneClientTest {
  private static final String API_KEY = "test-jev-key";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicInteger requestCount = new AtomicInteger();
  private HttpServer server;
  private volatile Stub stub;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/v1/systemone",
        exchange -> {
          requestCount.incrementAndGet();
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          Stub current = stub;
          if (current.delay().isPositive()) {
            try {
              Thread.sleep(current.delay().toMillis());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(current.status(), body.length == 0 ? -1 : body.length);
          if (body.length > 0) {
            exchange.getResponseBody().write(body);
          }
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Nested
  @DisplayName("성공 응답")
  class SuccessCase {

    @Test
    @DisplayName("choice, score, noul 질문을 v1 계약으로 보내고 응답을 공급자 중립 타입으로 변환한다")
    void evaluate_세_가지_질문을_변환할_수_있다() throws IOException {
      // given
      respond(
          200,
          """
          {
            "model": "jev-1.13.0",
            "answers": {
              "department": {
                "type": "choice",
                "choice": "technical",
                "probabilities": {"billing": 0.08, "technical": 0.85, "sales": 0.07},
                "confidence": 0.82
              },
              "sentiment": {
                "type": "score",
                "score": 1.56,
                "legend": {"0": "Negative", "1": "Mixed", "2": "Positive"},
                "probabilities": {"0": 0.1, "1": 0.24, "2": 0.66},
                "confidence": 0.63
              },
              "urgent": {"type": "noul", "noul": 0.91}
            },
            "usage": {"input_tokens": 312, "output_tokens": 48}
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
      assertThat(success.model()).isEqualTo("jev-1.13.0");
      assertThat(success.usage().inputTokens()).isEqualTo(312);
      assertThat(success.answer("department", SystemOneAnswer.Choice.class).choice())
          .isEqualTo("technical");
      assertThat(success.answer("sentiment", SystemOneAnswer.Score.class).score())
          .isEqualTo(1.56);
      assertThat(success.answer("urgent", SystemOneAnswer.Noul.class).probability())
          .isEqualTo(0.91);

      assertThat(authorization.get()).isEqualTo("Bearer " + API_KEY);
      JsonNode sent = objectMapper.readTree(requestBody.get());
      assertThat(sent.path("model").asText()).isEqualTo("jev-latest");
      assertThat(sent.path("state").asText()).isEqualTo("결제가 안 돼요");
      assertThat(sent.at("/questions/department/type").asText()).isEqualTo("choice");
      assertThat(sent.at("/questions/department/criteria/technical").asText()).isEqualTo("Bugs");
      assertThat(sent.at("/questions/sentiment/criteria").isArray()).isTrue();
      assertThat(sent.at("/questions/sentiment/criteria/2").asText()).isEqualTo("Positive");
      assertThat(sent.at("/questions/urgent").has("criteria")).isFalse();
    }
  }

  @Nested
  @DisplayName("실패 응답")
  class FailureCase {

    @ParameterizedTest(name = "HTTP {0} -> {1}")
    @CsvSource({
      "400, INVALID_REQUEST",
      "422, INVALID_REQUEST",
      "401, UNAUTHORIZED",
      "403, UNAUTHORIZED",
      "429, RATE_LIMITED",
      "500, PROVIDER_UNAVAILABLE",
      "529, PROVIDER_UNAVAILABLE",
      "404, INVALID_RESPONSE"
    })
    @DisplayName("비정상 HTTP 상태를 실패 유형으로 변환하고 오류 메시지를 담는다")
    void evaluate_비정상_상태를_실패로_변환할_수_있다(int status, SystemOneFailureType expected) {
      respond(status, "{\"error\":{\"message\":\"provider said no\"}}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(expected);
      assertThat(failure.httpStatus()).isEqualTo(status);
      assertThat(failure.message()).isEqualTo("provider said no");
    }

    @Test
    @DisplayName("응답이 readTimeout보다 늦으면 TIMEOUT 실패를 반환한다")
    void evaluate_응답_지연을_타임아웃으로_처리할_수_있다() {
      stub = new Stub(200, noulBody("jev-1.13.0"), Duration.ofMillis(1500));
      JevProperties properties = properties();
      properties.setReadTimeout(Duration.ofMillis(200));

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.TIMEOUT);
      assertThat(failure.retryable()).isTrue();
    }

    @Test
    @DisplayName("연결할 수 없는 주소면 TRANSPORT_ERROR 실패를 반환한다")
    void evaluate_연결_실패를_전송_오류로_처리할_수_있다() throws IOException {
      JevProperties properties = properties();
      properties.setBaseUrl("http://127.0.0.1:" + unusedPort());

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.TRANSPORT_ERROR);
      assertThat(failure.httpStatus()).isNull();
    }

    @Test
    @DisplayName("200이지만 JSON이 아니면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_해석_불가_응답을_처리할_수_있다() {
      respond(200, "not-json");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("요청한 질문 키의 응답이 없으면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_누락된_응답을_처리할_수_있다() {
      respond(200, "{\"model\":\"jev-1.13.0\",\"answers\":{}}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
      assertThat(failure.message()).contains("urgent");
    }

    @Test
    @DisplayName("응답 타입이 질문 타입과 다르면 INVALID_RESPONSE 실패를 반환한다")
    void evaluate_타입이_다른_응답을_처리할_수_있다() {
      respond(
          200,
          "{\"model\":\"jev-1.13.0\",\"answers\":{\"urgent\":{\"type\":\"choice\",\"noul\":0.5}}}");

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("응답 모델의 major 버전이 v1이 아니면 UNSUPPORTED_VERSION 실패를 반환한다")
    void evaluate_지원하지_않는_응답_버전을_처리할_수_있다() {
      respond(200, noulBody("jev-2.0.0"));

      Failure failure = failure(client(properties()).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.UNSUPPORTED_VERSION);
    }

    @Test
    @DisplayName("설정된 모델을 지원하지 않으면 요청을 보내지 않고 UNSUPPORTED_VERSION을 반환한다")
    void evaluate_지원하지_않는_설정_모델을_처리할_수_있다() {
      JevProperties properties = properties();
      properties.setModel("jev-2.0.0");

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.UNSUPPORTED_VERSION);
      assertThat(requestCount.get()).isZero();
    }

    @Test
    @DisplayName("API Key가 없으면 요청을 보내지 않고 NOT_CONFIGURED를 반환한다")
    void evaluate_API_Key_미설정을_처리할_수_있다() {
      JevProperties properties = properties();
      properties.setApiKey(" ");

      Failure failure = failure(client(properties).evaluate(noulRequest()));

      assertThat(failure.type()).isEqualTo(SystemOneFailureType.NOT_CONFIGURED);
      assertThat(requestCount.get()).isZero();
    }
  }

  private DefaultJevSystemOneClient client(JevProperties properties) {
    return new DefaultJevSystemOneClient(
        JevHttpTransport.create(RestClient.builder(), properties),
        new JevV1Mapper(objectMapper),
        properties,
        objectMapper);
  }

  private JevProperties properties() {
    JevProperties properties = new JevProperties();
    properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
    properties.setApiKey(API_KEY);
    properties.setConnectTimeout(Duration.ofSeconds(1));
    properties.setReadTimeout(Duration.ofSeconds(2));
    return properties;
  }

  private void respond(int status, String body) {
    stub = new Stub(status, body, Duration.ZERO);
  }

  private static SystemOneRequest noulRequest() {
    return SystemOneRequest.of(
        Map.of("text", "지금 바로 확인해 주세요"),
        "urgent",
        new SystemOneQuestion.Noul("Does this convey urgency?"));
  }

  private static String noulBody(String model) {
    return "{\"model\":\"%s\",\"answers\":{\"urgent\":{\"type\":\"noul\",\"noul\":0.7}}}"
        .formatted(model);
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

  private static int unusedPort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private record Stub(int status, String body, Duration delay) {}
}
