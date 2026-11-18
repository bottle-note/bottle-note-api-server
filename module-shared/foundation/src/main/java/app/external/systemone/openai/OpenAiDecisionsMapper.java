package app.external.systemone.openai;

import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneAnswer;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneUsage;
import app.external.systemone.http.SystemOneContractException;
import app.external.systemone.openai.dto.request.OpenAiDecisionsRequest;
import app.external.systemone.openai.dto.response.OpenAiDecisionsResponse;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * OpenAI Decisions API 계약 변환기. System One 질문을 Decisions 질문으로, Decisions 답변을 공급자 중립 답변으로 바꾼다.
 *
 * <p>대응 관계: Noul↔predicate, Choice↔choice, Score↔score. Decisions는 질문을 배열로 받으므로 질문 키를 {@code name}에
 * 싣고 응답에서 같은 이름으로 되찾는다.
 */
public class OpenAiDecisionsMapper {
  private static final String REFUSAL_TYPE = "refusal";

  private final ObjectMapper objectMapper;

  public OpenAiDecisionsMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * @throws SystemOneContractException state를 문자열로 직렬화할 수 없을 때
   */
  public OpenAiDecisionsRequest toRequest(SystemOneRequest request, String model) {
    List<OpenAiDecisionsRequest.Question> questions = new ArrayList<>();
    request.questions().forEach((key, question) -> questions.add(toQuestion(key, question)));
    return new OpenAiDecisionsRequest(model, toInput(request.state()), questions);
  }

  // Decisions의 input은 문자열 또는 메시지 배열이다. 문자열이 아닌 state는 JSON 텍스트로 넘긴다
  private Object toInput(Object state) {
    if (state instanceof String text) {
      return text;
    }
    try {
      return objectMapper.writeValueAsString(state);
    } catch (JsonProcessingException e) {
      throw new SystemOneContractException(
          SystemOneFailureType.INVALID_REQUEST, "state를 JSON으로 직렬화할 수 없습니다", e);
    }
  }

  private OpenAiDecisionsRequest.Question toQuestion(String key, SystemOneQuestion question) {
    return switch (question) {
      case SystemOneQuestion.Choice choice ->
          OpenAiDecisionsRequest.Question.choice(
              key,
              choice.instructions(),
              choice.options().entrySet().stream()
                  .map(e -> new OpenAiDecisionsRequest.Choice(e.getKey(), e.getValue()))
                  .toList());
      case SystemOneQuestion.Score score ->
          OpenAiDecisionsRequest.Question.score(
              key,
              score.instructions(),
              score.levels().stream()
                  .map(level -> new OpenAiDecisionsRequest.Level(level, level))
                  .toList());
      case SystemOneQuestion.Noul noul ->
          OpenAiDecisionsRequest.Question.predicate(key, noul.instructions());
    };
  }

  /**
   * @throws SystemOneContractException 응답이 계약과 다르거나 거부된 질문이 있을 때
   */
  public SystemOneResult.Success toResult(String body, SystemOneRequest request) {
    OpenAiDecisionsResponse response = parse(body);
    if (response.answers() == null) {
      throw invalid("answers가 없습니다");
    }
    Map<String, OpenAiDecisionsResponse.Answer> byName = new LinkedHashMap<>();
    for (OpenAiDecisionsResponse.Answer answer : response.answers()) {
      if (answer == null || answer.name() == null) {
        continue;
      }
      // 같은 name이 두 번 오면 순서에 따라 refusal이 묻힐 수 있으므로 계약 위반으로 거부한다
      if (byName.putIfAbsent(answer.name(), answer) != null) {
        throw invalid("질문 키 %s의 응답이 중복됩니다".formatted(answer.name()));
      }
    }

    List<String> refused = new ArrayList<>();
    Map<String, SystemOneAnswer> answers = new LinkedHashMap<>();
    request
        .questions()
        .forEach(
            (key, question) -> {
              OpenAiDecisionsResponse.Answer answer = byName.get(key);
              if (answer == null) {
                throw invalid("질문 키 %s의 응답이 없습니다".formatted(key));
              }
              if (REFUSAL_TYPE.equals(answer.type())) {
                refused.add(key);
                return;
              }
              answers.put(key, toAnswer(key, question, answer));
            });
    // 한 질문이라도 거부되면 결정 전체를 완성할 수 없으므로 부분 성공 대신 실패로 돌려준다
    if (!refused.isEmpty()) {
      throw new SystemOneContractException(
          SystemOneFailureType.REFUSED, "모델이 질문에 대한 답변을 거부했습니다: " + String.join(", ", refused));
    }
    String model = response.model() == null ? "" : response.model();
    return new SystemOneResult.Success(model, answers, toUsage(response.usage()));
  }

  private OpenAiDecisionsResponse parse(String body) {
    try {
      OpenAiDecisionsResponse response =
          objectMapper.readValue(body, OpenAiDecisionsResponse.class);
      if (response == null) {
        throw invalid("응답 본문이 비어 있습니다");
      }
      return response;
    } catch (JacksonException e) {
      throw invalid("응답 JSON을 해석할 수 없습니다: " + e.getOriginalMessage());
    }
  }

  private SystemOneAnswer toAnswer(
      String key, SystemOneQuestion question, OpenAiDecisionsResponse.Answer answer) {
    return switch (question) {
      case SystemOneQuestion.Choice ignored -> {
        requireType(key, answer, "choice");
        yield new SystemOneAnswer.Choice(
            require(key, "choice", answer.choice()),
            toProbabilities(key, answer),
            require(key, "confidence", answer.confidence()));
      }
      case SystemOneQuestion.Score score -> {
        requireType(key, answer, "score");
        yield new SystemOneAnswer.Score(
            require(key, "score", answer.score()),
            legendOf(score),
            toProbabilities(key, answer),
            require(key, "confidence", answer.confidence()));
      }
      case SystemOneQuestion.Noul ignored -> {
        requireType(key, answer, "predicate");
        yield new SystemOneAnswer.Noul(require(key, "probability", answer.probability()));
      }
    };
  }

  // Decisions 응답에는 legend가 없어 요청의 단계 순서로 복원한다
  private Map<String, String> legendOf(SystemOneQuestion.Score score) {
    Map<String, String> legend = new LinkedHashMap<>();
    IntStream.range(0, score.levels().size())
        .forEach(i -> legend.put(String.valueOf(i), score.levels().get(i)));
    return legend;
  }

  private Map<String, Double> toProbabilities(String key, OpenAiDecisionsResponse.Answer answer) {
    List<OpenAiDecisionsResponse.Probability> items =
        require(key, "probabilities", answer.probabilities());
    Map<String, Double> probabilities = new LinkedHashMap<>();
    for (OpenAiDecisionsResponse.Probability item : items) {
      if (item == null || item.value() == null) {
        throw invalid("질문 키 %s의 probabilities에 value가 없습니다".formatted(key));
      }
      probabilities.put(probabilityKey(item.value()), item.probability());
    }
    return probabilities;
  }

  // score는 단계 인덱스가 숫자로 오므로 Jev legend와 같은 "0", "1" 형태의 키로 맞춘다
  private String probabilityKey(Object value) {
    if (value instanceof Number number) {
      return String.valueOf(number.longValue());
    }
    return String.valueOf(value);
  }

  private void requireType(String key, OpenAiDecisionsResponse.Answer answer, String expected) {
    if (answer.type() != null && !expected.equals(answer.type())) {
      throw invalid("질문 키 %s의 응답 타입이 %s가 아닙니다: %s".formatted(key, expected, answer.type()));
    }
  }

  private <T> T require(String key, String field, T value) {
    if (value == null) {
      throw invalid("질문 키 %s의 응답에 %s가 없습니다".formatted(key, field));
    }
    return value;
  }

  private SystemOneUsage toUsage(OpenAiDecisionsResponse.Usage usage) {
    if (usage == null) {
      return SystemOneUsage.empty();
    }
    return new SystemOneUsage(
        usage.inputTokens() == null ? 0 : usage.inputTokens(),
        usage.outputTokens() == null ? 0 : usage.outputTokens());
  }

  private SystemOneContractException invalid(String message) {
    return SystemOneContractException.invalidResponse(message);
  }
}
