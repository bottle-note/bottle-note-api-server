package app.external.systemone.openai;

import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.http.AbstractHttpSystemOneClient;
import app.external.systemone.http.SystemOneHttpTransport;
import app.external.systemone.openai.config.OpenAiDecisionsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/** OpenAI Decisions API 기반 System One 클라이언트. 계약 변환은 {@link OpenAiDecisionsMapper}에 위임한다. */
public class DefaultOpenAiSystemOneClient extends AbstractHttpSystemOneClient {
  public static final String DECISIONS_PATH = "/v1/decisions";
  static final String REQUEST_ID_HEADER = "x-request-id";

  private final OpenAiDecisionsMapper mapper;

  public DefaultOpenAiSystemOneClient(
      SystemOneHttpTransport transport,
      OpenAiDecisionsMapper mapper,
      OpenAiDecisionsProperties properties,
      ObjectMapper objectMapper) {
    super("OpenAI", transport, properties, objectMapper);
    this.mapper = mapper;
  }

  @Override
  protected String requestIdHeader() {
    return REQUEST_ID_HEADER;
  }

  @Override
  protected Object toProviderRequest(SystemOneRequest request) {
    return mapper.toRequest(request, properties().getModel());
  }

  @Override
  protected SystemOneResult.Success toResult(String body, SystemOneRequest request) {
    return mapper.toResult(body, request);
  }
}
