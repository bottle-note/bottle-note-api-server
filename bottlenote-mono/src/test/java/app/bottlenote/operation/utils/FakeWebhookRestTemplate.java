package app.bottlenote.operation.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/** 테스트용 Fake RestTemplate 구현체. 실제 HTTP 호출 없이 요청을 캡처하여 검증에 사용합니다. */
public class FakeWebhookRestTemplate extends RestTemplate {

  private final List<CapturedRequest> capturedRequests =
      Collections.synchronizedList(new ArrayList<>());
  private ResponseEntity<?> defaultResponse = ResponseEntity.ok("Success");

  @Override
  @SuppressWarnings("unchecked")
  public <T> ResponseEntity<T> postForEntity(
      String url, Object request, Class<T> responseType, Object... uriVariables) {
    capturedRequests.add(new CapturedRequest(url, request));
    return (ResponseEntity<T>) defaultResponse;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ResponseEntity<T> postForEntity(
      String url, Object request, Class<T> responseType, java.util.Map<String, ?> uriVariables) {
    capturedRequests.add(new CapturedRequest(url, request));
    return (ResponseEntity<T>) defaultResponse;
  }

  public void setDefaultResponse(ResponseEntity<?> response) {
    this.defaultResponse = response;
  }

  public int getCallCount() {
    return capturedRequests.size();
  }

  public boolean wasCalled() {
    return !capturedRequests.isEmpty();
  }

  public boolean wasNotCalled() {
    return capturedRequests.isEmpty();
  }

  public CapturedRequest getLastRequest() {
    if (capturedRequests.isEmpty()) {
      return null;
    }
    return capturedRequests.get(capturedRequests.size() - 1);
  }

  public String getLastRequestBody() {
    CapturedRequest last = getLastRequest();
    if (last == null || last.request() == null) {
      return null;
    }
    if (last.request() instanceof HttpEntity<?> entity) {
      return entity.getBody() != null ? entity.getBody().toString() : null;
    }
    return last.request().toString();
  }

  public List<CapturedRequest> getAllRequests() {
    return Collections.unmodifiableList(capturedRequests);
  }

  public void clear() {
    capturedRequests.clear();
    defaultResponse = ResponseEntity.ok("Success");
  }

  public record CapturedRequest(String url, Object request) {}
}
