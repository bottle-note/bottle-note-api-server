package app.bottlenote.operation.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

/** 테스트용 Fake RestTemplate 구현체. 실제 HTTP 호출 없이 요청을 캡처하여 검증에 사용합니다. */
public class FakeWebhookRestTemplate extends RestTemplate {

  private final List<CapturedCall> capturedCalls = Collections.synchronizedList(new ArrayList<>());
  private final ResponseEntity<?> defaultResponse = ResponseEntity.ok("Success");

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> ResponseEntity<T> postForEntity(
      @NonNull String url,
      Object request,
      @NonNull Class<T> responseType,
      @NonNull Object... uriVariables) {
    capturedCalls.add(new CapturedCall(url, request));
    return (ResponseEntity<T>) defaultResponse;
  }

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> ResponseEntity<T> postForEntity(
      @NonNull String url,
      Object request,
      @NonNull Class<T> responseType,
      @NonNull Map<String, ?> uriVariables) {
    capturedCalls.add(new CapturedCall(url, request));
    return (ResponseEntity<T>) defaultResponse;
  }

  public int getCallCount() {
    return capturedCalls.size();
  }

  public boolean wasCalled() {
    return !capturedCalls.isEmpty();
  }

  public boolean wasNotCalled() {
    return capturedCalls.isEmpty();
  }

  public String getLastRequestBody() {
    if (capturedCalls.isEmpty()) {
      return null;
    }
    CapturedCall last = capturedCalls.getLast();
    if (last.payload() == null) {
      return null;
    }
    if (last.payload() instanceof HttpEntity<?> entity) {
      return entity.getBody() != null ? entity.getBody().toString() : null;
    }
    return last.payload().toString();
  }

  public void clear() {
    capturedCalls.clear();
  }

  public record CapturedCall(String url, Object payload) {}
}
