package app.bottlenote.global.security.accesscontrol;

import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.accesscontrol.AccessControlService.Decision;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT 이전에 IP ban / rate limit을 검사한다. */
public class AccessControlFilter extends OncePerRequestFilter {

  private final AccessControlService accessControlService;
  private final ObjectMapper objectMapper;

  public AccessControlFilter(AccessControlService accessControlService, ObjectMapper objectMapper) {
    this.accessControlService = accessControlService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientIp = ClientIpResolver.resolve(request);
    String path = resolvePathWithinApplication(request);
    Decision decision = accessControlService.evaluate(clientIp, path);

    if (decision.allowed()) {
      if (decision.limit() >= 0) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
      }
      if (decision.remaining() >= 0) {
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
      }
      filterChain.doFilter(request, response);
      return;
    }

    if (decision.type() == Decision.Type.BANNED) {
      writeError(
          response,
          HttpStatus.FORBIDDEN,
          AccessControlExceptionCode.IP_BANNED,
          decision.retryAfterSeconds(),
          decision.limit());
      return;
    }

    writeError(
        response,
        HttpStatus.TOO_MANY_REQUESTS,
        AccessControlExceptionCode.RATE_LIMIT_EXCEEDED,
        decision.retryAfterSeconds(),
        decision.limit());
  }

  /** context-path를 제거해 admin/product path-rules가 동일 규칙으로 매칭되게 한다. */
  static String resolvePathWithinApplication(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null) {
      return "/";
    }
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      String path = uri.substring(contextPath.length());
      return path.isEmpty() ? "/" : path;
    }
    return uri;
  }

  private void writeError(
      HttpServletResponse response,
      HttpStatus status,
      AccessControlExceptionCode code,
      long retryAfterSeconds,
      long limit)
      throws IOException {
    response.setStatus(status.value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    if (retryAfterSeconds > 0) {
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    }
    if (limit >= 0) {
      response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
    }

    Error error = Error.of(code);
    GlobalResponse body =
        GlobalResponse.builder()
            .success(false)
            .code(status.value())
            .data(List.of())
            .errors(List.of(error))
            .meta(createMetaInfo().getMetaInfos())
            .build();
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
