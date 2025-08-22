package app.bottlenote.common.loger;

import feign.Logger;
import feign.Request;
import feign.Response;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FeginCustomLogger extends Logger {

  @Override
  protected void log(String configKey, String format, Object... args) {
    log.info("{}{}%n", methodTag(configKey), format, args);
  }

  @Override
  protected void logRequest(String configKey, Level logLevel, Request request) {
    System.out.println("Request: " + request.toString());
  }

  @Override
  protected Response logAndRebufferResponse(
      String configKey, Level logLevel, Response response, long elapsedTime) throws IOException {
    String protocolVersion = resolveProtocolVersion(response.protocolVersion());

    if (response.reason() != null && logLevel.compareTo(Level.NONE) > 0)
      log.info(
          "<--- HTTP/{} {} ({}ms, {}-byte body)",
          protocolVersion,
          response.status(),
          elapsedTime,
          response.body().length());
    else log.info("<--- HTTP/{} {} ({}ms)", protocolVersion, response.status(), elapsedTime);

    return super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
  }
}
