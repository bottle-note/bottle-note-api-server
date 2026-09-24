package app.external.systemone.jev.v1;

import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneAnswer;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneUsage;
import app.external.systemone.jev.v1.dto.request.JevV1Request;
import app.external.systemone.jev.v1.dto.response.JevV1Response;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Jev API v1 계약 변환기. 공급자 계약이 바뀌면 이 클래스와 v1 DTO만 교체하거나 v2를 추가한다.
 *
 * <p>jev-latest는 서버가 실제 버전(jev-1.13.0 등)으로 해석하므로 응답 모델의 major 버전을 다시 검증한다.
 */
public class JevV1Mapper {
  private static final String LATEST_ALIAS = "jev-latest";
  private static final Pattern SUPPORTED_MODEL = Pattern.compile("^jev-1(\\.\\d+){0,2}$");

  private final ObjectMapper objectMapper;

  public JevV1Mapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public boolean supportsModel(String model) {
    return model != null && (LATEST_ALIAS.equals(model) || isSupportedVersion(model));
  }

  public JevV1Request toRequest(SystemOneRequest request, String model) {
    Map<String, JevV1Request.Question> questions = new LinkedHashMap<>();
    request.questions().forEach((key, question) -> questions.put(key, toQuestion(question)));
    return new JevV1Request(request.state(), model, questions);
  }

  private JevV1Request.Question toQuestion(SystemOneQuestion question) {
    return switch (question) {
      case SystemOneQuestion.Choice choice ->
          new JevV1Request.Question("choice", choice.instructions(), choice.options());
      case SystemOneQuestion.Score score ->
          new JevV1Request.Question("score", score.instructions(), score.levels());
      case SystemOneQuestion.Noul noul -> new JevV1Request.Question("noul", noul.instructions(), null);
    };
  }

  /** @throws JevContractException 응답이 v1 계약과 다르거나 지원하지 않는 버전일 때 */
  public SystemOneResult.Success toResult(String body, SystemOneRequest request) {
    JevV1Response response = parse(body);
    if (!isSupportedVersion(response.model())) {
      throw new JevContractException(
          SystemOneFailureType.UNSUPPORTED_VERSION, "지원하지 않는 응답 모델: " + response.model());
    }
    if (response.answers() == null) {
      throw invalid("answers가 없습니다");
    }

    Map<String, SystemOneAnswer> answers = new LinkedHashMap<>();
    request
        .questions()
        .forEach(
            (key, question) -> {
              JevV1Response.Answer answer = response.answers().get(key);
              if (answer == null) {
                throw invalid("질문 키 %s의 응답이 없습니다".formatted(key));
              }
              answers.put(key, toAnswer(key, question, answer));
            });
    return new SystemOneResult.Success(response.model(), answers, toUsage(response.usage()));
  }

  private JevV1Response parse(String body) {
    try {
      JevV1Response response = objectMapper.readValue(body, JevV1Response.class);
      if (response == null) {
        throw invalid("응답 본문이 비어 있습니다");
      }
      return response;
    } catch (JacksonException e) {
      throw invalid("응답 JSON을 해석할 수 없습니다: " + e.getOriginalMessage());
    }
  }

  private SystemOneAnswer toAnswer(
      String key, SystemOneQuestion question, JevV1Response.Answer answer) {
    return switch (question) {
      case SystemOneQuestion.Choice ignored -> {
        requireType(key, answer, "choice");
        yield new SystemOneAnswer.Choice(
            require(key, "choice", answer.choice()),
            require(key, "probabilities", answer.probabilities()),
            require(key, "confidence", answer.confidence()));
      }
      case SystemOneQuestion.Score ignored -> {
        requireType(key, answer, "score");
        yield new SystemOneAnswer.Score(
            require(key, "score", answer.score()),
            require(key, "legend", answer.legend()),
            require(key, "probabilities", answer.probabilities()),
            require(key, "confidence", answer.confidence()));
      }
      case SystemOneQuestion.Noul ignored -> {
        requireType(key, answer, "noul");
        yield new SystemOneAnswer.Noul(require(key, "noul", answer.noul()));
      }
    };
  }

  private void requireType(String key, JevV1Response.Answer answer, String expected) {
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

  private SystemOneUsage toUsage(JevV1Response.Usage usage) {
    if (usage == null) {
      return SystemOneUsage.empty();
    }
    return new SystemOneUsage(
        usage.inputTokens() == null ? 0 : usage.inputTokens(),
        usage.outputTokens() == null ? 0 : usage.outputTokens());
  }

  private boolean isSupportedVersion(String model) {
    return model != null && SUPPORTED_MODEL.matcher(model).matches();
  }

  private JevContractException invalid(String message) {
    return new JevContractException(SystemOneFailureType.INVALID_RESPONSE, message);
  }

  /** v1 계약 위반. 클라이언트 경계 밖으로 나가지 않고 Failure로 변환된다. */
  @Getter
  public static class JevContractException extends RuntimeException {
    private final SystemOneFailureType type;

    public JevContractException(SystemOneFailureType type, String message) {
      super(message);
      this.type = type;
    }
  }
}
