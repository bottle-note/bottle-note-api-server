package app.external.systemone.jev;

import app.external.systemone.jev.config.JevProperties;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Jev HTTP 전송 계층. 상태 코드 해석과 본문 변환은 하지 않고 원본 응답만 돌려준다. */
public class JevHttpTransport {
  static final String SYSTEM_ONE_PATH = "/v1/systemone";

  private final RestClient restClient;

  public JevHttpTransport(RestClient restClient) {
    this.restClient = restClient;
  }

  public static JevHttpTransport create(RestClient.Builder builder, JevProperties properties) {
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
    return new JevHttpTransport(restClient);
  }

  /** 전송 실패 시 RestClient의 {@code ResourceAccessException}을 그대로 던진다. */
  public RawResponse post(Object body) {
    return restClient
        .post()
        .uri(SYSTEM_ONE_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
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
  }
}
