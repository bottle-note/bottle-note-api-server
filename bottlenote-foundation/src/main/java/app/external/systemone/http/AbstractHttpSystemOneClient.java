package app.external.systemone.http;

import app.external.systemone.SystemOneClient;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneResult.Failure;
import app.external.systemone.http.SystemOneHttpTransport.RawResponse;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * HTTP 공급자 공통 골격. 설정 검증, 전송 오류와 HTTP 상태의 실패 분류, 로깅을 담당하고 계약 변환은 공급자 구현체에 맡긴다.
 *
 * <p>공급자 오류는 모두 {@link Failure}로 돌려준다. 호출 측 입력 오류(직렬화 불가 state 등)도 예외 대신 {@code INVALID_REQUEST}로
 * 돌려준다.
 */
@Slf4j
public abstract class AbstractHttpSystemOneClient implements SystemOneClient {
  private static final int MAX_ERROR_MESSAGE_LENGTH = 300;
  static final String SERIALIZATION_FAILURE_MESSAGE = "요청 본문을 JSON으로 직렬화할 수 없습니다";

  private final String providerName;
  private final SystemOneHttpTransport transport;
  private final SystemOneProviderProperties properties;
  private final ObjectMapper objectMapper;

  protected AbstractHttpSystemOneClient(
      String providerName,
      SystemOneHttpTransport transport,
      SystemOneProviderProperties properties,
      ObjectMapper objectMapper) {
    this.providerName = providerName;
    this.transport = transport;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public final SystemOneResult evaluate(SystemOneRequest request) {
    if (!properties.hasApiKey()) {
      return Failure.of(SystemOneFailureType.NOT_CONFIGURED, providerName + " API Key가 설정되지 않았습니다");
    }
    Optional<Failure> rejected = validateConfiguration();
    if (rejected.isPresent()) {
      return rejected.get();
    }

    long start = System.nanoTime();
    Called called = call(request);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    if (called.result() instanceof Failure failure) {
      log.warn(
          "[SystemOne:{}] 호출 실패 type={}, status={}, requestId={}, elapsedMs={}, message={}",
          providerName,
          failure.type(),
          failure.httpStatus(),
          called.requestId(),
          elapsedMs,
          failure.message());
    } else {
      log.debug(
          "[SystemOne:{}] 호출 성공 requestId={}, elapsedMs={}",
          providerName,
          called.requestId(),
          elapsedMs);
    }
    return called.result();
  }

  protected SystemOneProviderProperties properties() {
    return properties;
  }

  /** 요청을 보내기 전 설정 상태를 검증한다. 거부할 이유가 없으면 비어 있다. */
  protected Optional<Failure> validateConfiguration() {
    return Optional.empty();
  }

  /** 공급자 응답에서 요청 ID를 읽을 헤더 이름. 없으면 null. */
  protected abstract String requestIdHeader();

  /**
   * @throws SystemOneContractException 요청을 공급자 형식으로 바꿀 수 없을 때
   */
  protected abstract Object toProviderRequest(SystemOneRequest request);

  /**
   * @throws SystemOneContractException 응답이 공급자 계약과 다를 때
   */
  protected abstract SystemOneResult.Success toResult(String body, SystemOneRequest request);

  private Called call(SystemOneRequest request) {
    byte[] body;
    try {
      body = objectMapper.writeValueAsBytes(toProviderRequest(request));
    } catch (SystemOneContractException e) {
      // 예외 원문에 state 값이 섞일 수 있어 Failure 메시지에는 담지 않고 debug 로그로만 남긴다
      log.debug("[SystemOne:{}] 요청 변환 실패", providerName, e);
      return Called.of(Failure.of(e.getType(), e.getMessage()));
    } catch (JsonProcessingException e) {
      log.debug("[SystemOne:{}] 요청 본문 직렬화 실패", providerName, e);
      return Called.of(
          Failure.of(SystemOneFailureType.INVALID_REQUEST, SERIALIZATION_FAILURE_MESSAGE));
    }

    RawResponse response;
    try {
      response = transport.post(body);
    } catch (ResourceAccessException e) {
      return Called.of(transportFailure(e));
    } catch (RestClientException e) {
      return Called.of(Failure.of(SystemOneFailureType.TRANSPORT_ERROR, e.getMessage()));
    }

    String requestId = requestIdHeader() == null ? null : response.header(requestIdHeader());
    if (!response.isSuccessful()) {
      return new Called(httpFailure(response), requestId);
    }
    try {
      return new Called(toResult(response.body(), request), requestId);
    } catch (SystemOneContractException e) {
      return new Called(new Failure(e.getType(), response.status(), e.getMessage()), requestId);
    }
  }

  private Failure transportFailure(ResourceAccessException e) {
    if (isTimeout(e)) {
      return Failure.of(SystemOneFailureType.TIMEOUT, e.getMessage());
    }
    return Failure.of(SystemOneFailureType.TRANSPORT_ERROR, e.getMessage());
  }

  private boolean isTimeout(Throwable e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }

  private Failure httpFailure(RawResponse response) {
    int status = response.status();
    SystemOneFailureType type =
        switch (status) {
          case 400, 422 -> SystemOneFailureType.INVALID_REQUEST;
          case 401, 403 -> SystemOneFailureType.UNAUTHORIZED;
          case 429 -> SystemOneFailureType.RATE_LIMITED;
          default ->
              status >= 500
                  ? SystemOneFailureType.PROVIDER_UNAVAILABLE
                  : SystemOneFailureType.INVALID_RESPONSE;
        };
    return new Failure(type, status, errorMessage(response.body()));
  }

  // 오류 본문 형식이 공급자마다 달라 흔한 위치를 순서대로 찾고, 없으면 원문 일부를 쓴다
  private String errorMessage(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode error = root.path("error");
      for (JsonNode candidate :
          new JsonNode[] {
            error.path("message"), root.path("message"), root.path("detail"), error
          }) {
        if (candidate.isTextual()) {
          return truncate(candidate.asText());
        }
      }
    } catch (JacksonException e) {
      log.debug("[SystemOne:{}] 오류 본문이 JSON이 아니어서 원문을 사용한다", providerName);
    }
    return truncate(body);
  }

  private String truncate(String value) {
    return value.length() <= MAX_ERROR_MESSAGE_LENGTH
        ? value
        : value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }

  private record Called(SystemOneResult result, String requestId) {
    static Called of(SystemOneResult result) {
      return new Called(result, null);
    }
  }
}
