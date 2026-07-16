package app.bottlenote.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRequestConsoleLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startedAt;
      log.info(
          "api_request method={} path={} queryPresent={} status={} durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          hasQuery(request),
          response.getStatus(),
          durationMs);
    }
  }

  private boolean hasQuery(HttpServletRequest request) {
    String queryString = request.getQueryString();
    return queryString != null && !queryString.isBlank();
  }
}
