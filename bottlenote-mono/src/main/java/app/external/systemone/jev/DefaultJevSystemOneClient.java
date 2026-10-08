package app.external.systemone.jev;

import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.dto.response.SystemOneResult.Failure;
import app.external.systemone.http.AbstractHttpSystemOneClient;
import app.external.systemone.http.SystemOneHttpTransport;
import app.external.systemone.jev.config.JevProperties;
import app.external.systemone.jev.v1.JevV1Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

/** TypeSafe Jev 기반 System One 클라이언트. 계약 변환은 {@link JevV1Mapper}에 위임한다. */
public class DefaultJevSystemOneClient extends AbstractHttpSystemOneClient {
  public static final String SYSTEM_ONE_PATH = "/v1/systemone";
  static final String REQUEST_ID_HEADER = "x-typesafe-request-id";

  private final JevV1Mapper mapper;

  public DefaultJevSystemOneClient(
      SystemOneHttpTransport transport,
      JevV1Mapper mapper,
      JevProperties properties,
      ObjectMapper objectMapper) {
    super("Jev", transport, properties, objectMapper);
    this.mapper = mapper;
  }

  @Override
  protected Optional<Failure> validateConfiguration() {
    String model = properties().getModel();
    if (!mapper.supportsModel(model)) {
      return Optional.of(
          Failure.of(SystemOneFailureType.UNSUPPORTED_VERSION, "설정된 모델을 지원하지 않습니다: " + model));
    }
    return Optional.empty();
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
