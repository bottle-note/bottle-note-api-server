package app.external.systemone;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** 공급자 HTTP 계약을 재현하는 로컬 서버. 마지막 요청의 본문과 인증 헤더를 기록한다. */
public final class StubProviderServer implements AutoCloseable {
  private final HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicInteger requestCount = new AtomicInteger();
  private volatile Stub stub = new Stub(200, "{}", Duration.ZERO);

  public StubProviderServer(String path) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        path,
        exchange -> {
          requestCount.incrementAndGet();
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          Stub current = stub;
          if (current.delay().isPositive()) {
            try {
              Thread.sleep(current.delay().toMillis());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(current.status(), body.length == 0 ? -1 : body.length);
          if (body.length > 0) {
            exchange.getResponseBody().write(body);
          }
          exchange.close();
        });
    server.start();
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public void respond(int status, String body) {
    stub = new Stub(status, body, Duration.ZERO);
  }

  public void respondAfter(Duration delay, int status, String body) {
    stub = new Stub(status, body, delay);
  }

  public String lastRequestBody() {
    return requestBody.get();
  }

  public String lastAuthorization() {
    return authorization.get();
  }

  public int requestCount() {
    return requestCount.get();
  }

  public static String unusedBaseUrl() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return "http://127.0.0.1:" + socket.getLocalPort();
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private record Stub(int status, String body, Duration delay) {}
}
