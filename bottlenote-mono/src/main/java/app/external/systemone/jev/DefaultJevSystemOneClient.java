package app.external.systemone.jev;

import app.external.systemone.SystemOneClient;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneResult.Failure;
import app.external.systemone.jev.JevHttpTransport.RawResponse;
import app.external.systemone.jev.config.JevProperties;
import app.external.systemone.jev.v1.JevV1Mapper;
import app.external.systemone.jev.v1.JevV1Mapper.JevContractException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;

/** TypeSafe Jev 기반 System One 클라이언트. 모든 공급자 오류를 {@link Failure}로 변환한다. */
@Slf4j
public class DefaultJevSystemOneClient implements SystemOneClient {
  private static final int MAX_ERROR_MESSAGE_LENGTH = 300;

  private final JevHttpTransport transport;
  private final JevV1Mapper mapper;
  private final JevProperties properties;
  private final ObjectMapper objectMapper;

  public DefaultJevSystemOneClient(
      JevHttpTransport transport,
      JevV1Mapper mapper,
      JevProperties properties,
      ObjectMapper objectMapper) {
    this.transport = transport;
    this.mapper = mapper;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public SystemOneResult evaluate(SystemOneRequest request) {
    if (!properties.hasApiKey()) {
      return Failure.of(SystemOneFailureType.NOT_CONFIGURED, "systemone.jev.api-key가 설정되지 않았습니다");
    }
    if (!mapper.supportsModel(properties.getModel())) {
      return Failure.of(
          SystemOneFailureType.UNSUPPORTED_VERSION,
          "설정된 모델을 지원하지 않습니다: " + properties.getModel());
    }

    long start = System.nanoTime();
    SystemOneResult result = call(request);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    if (result instanceof Failure failure) {
      log.warn(
          "[SystemOne:Jev] 호출 실패 type={}, status={}, elapsedMs={}, message={}",
          failure.type(),
          failure.httpStatus(),
          elapsedMs,
          failure.message());
    } else {
      log.debug("[SystemOne:Jev] 호출 성공 elapsedMs={}", elapsedMs);
    }
    return result;
  }

  private SystemOneResult call(SystemOneRequest request) {
    RawResponse response;
    try {
      response = transport.post(mapper.toRequest(request, properties.getModel()));
    } catch (ResourceAccessException e) {
      return transportFailure(e);
    }

    if (!response.isSuccessful()) {
      return httpFailure(response);
    }
    try {
      return mapper.toResult(response.body(), request);
    } catch (JevContractException e) {
      return new Failure(e.getType(), response.status(), e.getMessage());
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

  // 오류 본문 형식이 공개 계약으로 고정되지 않아 해석 실패 시 원문 일부를 쓴다
  private String errorMessage(String body) {
    if (body == null || body.isBlank()) {
      return "";
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode error = root.path("error");
      for (JsonNode candidate :
          new JsonNode[] {error.path("message"), root.path("message"), root.path("detail"), error}) {
        if (candidate.isTextual()) {
          return truncate(candidate.asText());
        }
      }
    } catch (JacksonException e) {
      log.debug("[SystemOne:Jev] 오류 본문이 JSON이 아니어서 원문을 사용한다");
    }
    return truncate(body);
  }

  private String truncate(String value) {
    return value.length() <= MAX_ERROR_MESSAGE_LENGTH
        ? value
        : value.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }
}
