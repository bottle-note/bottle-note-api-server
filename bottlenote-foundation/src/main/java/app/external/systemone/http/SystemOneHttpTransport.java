package app.external.systemone.http;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 공급자 공통 HTTP 전송 계층. 상태 코드 해석과 본문 변환은 하지 않고 원본 응답만 돌려준다. */
public class SystemOneHttpTransport {

  private final RestClient restClient;
  private final String path;

  public SystemOneHttpTransport(RestClient restClient, String path) {
    this.restClient = restClient;
    this.path = path;
  }

  public static SystemOneHttpTransport create(
      RestClient.Builder builder, SystemOneProviderProperties properties, String path) {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getReadTimeout());

    RestClient restClient =
        builder
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .defaultHeaders(
                headers -> {
                  if (properties.hasApiKey()) {
                    headers.setBearerAuth(properties.getApiKey());
                  }
                })
            .build();
    return new SystemOneHttpTransport(restClient, path);
  }

  /**
   * 직렬화가 끝난 JSON 본문을 보낸다. 네트워크 I/O 전에 직렬화 오류를 걸러내기 위해 객체가 아니라 바이트를 받는다.
   *
   * <p>전송 실패 시 RestClient의 {@code ResourceAccessException}을 그대로 던진다.
   */
  public RawResponse post(byte[] jsonBody) {
    return restClient
        .post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(jsonBody)
        .exchange(
            (request, response) ->
                new RawResponse(
                    response.getStatusCode().value(),
                    response.getHeaders(),
                    new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
  }

  public record RawResponse(int status, HttpHeaders headers, String body) {
    public boolean isSuccessful() {
      return status >= 200 && status < 300;
    }

    public String header(String name) {
      return headers == null ? null : headers.getFirst(name);
    }
  }
}
